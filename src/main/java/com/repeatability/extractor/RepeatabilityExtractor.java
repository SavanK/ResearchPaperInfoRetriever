package com.repeatability.extractor;

import com.repeatability.extractor.extractors.AffiliationExtractor;
import com.repeatability.extractor.extractors.ArtifactExtractor;
import com.repeatability.extractor.extractors.EmailExtractor;
import com.repeatability.extractor.extractors.FundingExtractor;
import com.repeatability.extractor.matchmakers.AffiliationMatchMaker;
import com.repeatability.extractor.matchmakers.EmailMatchMaker;
import com.repeatability.extractor.pdfparsers.pdf2text.Pdf2Text;
import com.repeatability.extractor.pdfparsers.pdf2xml.Pdf2Xml;
import com.repeatability.extractor.pdfparsers.pdfinfo.PdfInfo;
import com.repeatability.extractor.scorer.ArtifactScorer;
import com.repeatability.extractor.scorer.FundingScorer;
import com.repeatability.extractor.util.Utf8;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by savan on 5/24/17.
 */
public class RepeatabilityExtractor implements AutoCloseable {
    private Properties prop;
    private ExecutorService executorService;

    public RepeatabilityExtractor() {
        prop = new Properties();
        try {
            BasicConfigurator.configure();
            InputStreamReader input = Utf8.newInputStreamReader(new FileInputStream(Config.SRC_DIR + getClass().getName().replace('.', '/') + ".properties"));
            prop.load(input);
            Logger.getLogger("org.apache.pdfbox").setLevel(Level.ERROR);
            Logger.getLogger("org.apache.fontbox").setLevel(Level.ERROR);
            Logger.getLogger("edu.stanford.nlp").setLevel(Level.ERROR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService = Executors.newFixedThreadPool(4);
        ((ThreadPoolExecutor)executorService).setKeepAliveTime(2, TimeUnit.MINUTES);
        ((ThreadPoolExecutor)executorService).setCorePoolSize(4);
        ((ThreadPoolExecutor)executorService).prestartCoreThread();
    }

    public PaperInfo extractInfo(String paperPath, String key, List<String> authors) {
        PaperInfo paperInfo = new PaperInfo(paperPath, key, authors);

        FutureTask<PdfInfo> pdfInfoFuture = new FutureTask<>(new PdfInfoCallable(paperPath));
        FutureTask<Pdf2Text> pdf2TextFuture = new FutureTask<>(new Pdf2TextCallable(paperPath));
        FutureTask<Pdf2Xml> pdf2XmlFuture = new FutureTask<>(new Pdf2XmlCallable(paperPath));

        executorService.execute(pdfInfoFuture);
        executorService.execute(pdf2TextFuture);
        executorService.execute(pdf2XmlFuture);

        try {
            paperInfo.setPdfInfo(pdfInfoFuture.get());
            paperInfo.setPdf2Text(pdf2TextFuture.get());
            paperInfo.setPdf2Xml(pdf2XmlFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        /**
         * Email extraction & matching
         * Affiliation extraction & matching
         * Artifact extraction & scoring
         * Funding extraction & scoring
         */
        FutureTask<Map<String, PaperInfo.MatchedEmail>> emailFuture = new FutureTask<>(new EmailCallable(paperInfo));
        FutureTask<Map<String, PaperInfo.MatchedAffiliation>> affiliationFuture = new FutureTask<>(new AffiliationCallable(paperInfo));
        FutureTask<List<PaperInfo.MatchedArtifact>> artifactFuture = new FutureTask<>(new ArtifactCallable(paperInfo));
        FutureTask<List<PaperInfo.MatchedFunding>> fundingFuture = new FutureTask<>(new FundingCallable(paperInfo));

        executorService.execute(emailFuture);
        executorService.execute(affiliationFuture);
        executorService.execute(artifactFuture);
        executorService.execute(fundingFuture);

        try {
            paperInfo.setAuthorEmails(emailFuture.get());
            paperInfo.setAuthorAffiliations(affiliationFuture.get());
            paperInfo.setArtifacts(artifactFuture.get());
            paperInfo.setFundings(fundingFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return paperInfo;
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    private class PdfInfoCallable implements Callable<PdfInfo> {
        private String paperPath;

        public PdfInfoCallable(String paperPath) {
            this.paperPath = paperPath;
        }

        @Override
        public PdfInfo call() throws Exception {
            return extractPdfInfo(paperPath);
        }
    }

    private class Pdf2TextCallable implements Callable<Pdf2Text> {
        private String paperPath;

        public Pdf2TextCallable(String paperPath) {
            this.paperPath = paperPath;
        }

        @Override
        public Pdf2Text call() throws Exception {
            return convertPdfToText(paperPath);
        }
    }

    private class Pdf2XmlCallable implements Callable<Pdf2Xml> {
        private String paperPath;

        public Pdf2XmlCallable(String paperPath) {
            this.paperPath = paperPath;
        }

        @Override
        public Pdf2Xml call() throws Exception {
            return convertPdfToXml(paperPath);
        }
    }

    private static class EmailCallable implements Callable<Map<String, PaperInfo.MatchedEmail>> {

        private PaperInfo paperInfo;

        public EmailCallable(PaperInfo paperInfo) {
            this.paperInfo = paperInfo;
        }

        @Override
        public Map<String, PaperInfo.MatchedEmail> call() throws Exception {
            List<String> emails = new EmailExtractor().extract(paperInfo);
            return new EmailMatchMaker().match(emails, paperInfo);
        }
    }

    private static class AffiliationCallable implements Callable<Map<String, PaperInfo.MatchedAffiliation>> {

        private PaperInfo paperInfo;
        private AffiliationExtractor affiliationExtractor;
        private AffiliationMatchMaker affiliationMatchMaker;

        public AffiliationCallable(PaperInfo paperInfo) {
            this.paperInfo = paperInfo;
            affiliationExtractor = new AffiliationExtractor();
            affiliationMatchMaker = new AffiliationMatchMaker();
        }

        @Override
        public Map<String, PaperInfo.MatchedAffiliation> call() throws Exception {
            return affiliationMatchMaker.match(affiliationExtractor.extract(paperInfo), paperInfo);
        }
    }

    private static class ArtifactCallable implements Callable<List<PaperInfo.MatchedArtifact>> {

        private PaperInfo paperInfo;
        private ArtifactExtractor artifactExtractor;
        private ArtifactScorer artifactScorer;

        public ArtifactCallable(PaperInfo paperInfo) {
            this.paperInfo = paperInfo;
            artifactExtractor = new ArtifactExtractor();
            artifactScorer = new ArtifactScorer();
        }

        @Override
        public List<PaperInfo.MatchedArtifact> call() throws Exception {
            return artifactScorer.scoreMatches(artifactExtractor.extract(paperInfo));
        }
    }

    private static class FundingCallable implements Callable<List<PaperInfo.MatchedFunding>> {

        private PaperInfo paperInfo;
        private FundingExtractor fundingExtractor;
        private FundingScorer fundingScorer;

        public FundingCallable(PaperInfo paperInfo) {
            this.paperInfo = paperInfo;
            fundingExtractor = new FundingExtractor();
            fundingScorer = new FundingScorer();
        }

        @Override
        public List<PaperInfo.MatchedFunding> call() throws Exception {
            return fundingScorer.scoreMatches(fundingExtractor.extract(paperInfo));
        }
    }

    protected PdfInfo extractPdfInfo(String paperPath) throws IOException {
        String commandBuilder = new StringBuilder()
                .append(prop.getProperty("pdfinfo"))
                .append(" ")
                .append(prop.getProperty("papersDirectory"))
                .append(paperPath)
                .toString();
        CommandLine command = CommandLine.parse(commandBuilder);
        ByteArrayOutputStream outputStream = Utf8.newByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(Config.WORK_DIR));
        executor.setStreamHandler(streamHandler);

        int exitValue = executor.execute(command);

        PdfInfo pdfInfo = null;
        if(exitValue == 0) {
            pdfInfo = new PdfInfo();
            pdfInfo.parse(outputStream.toString());
        }
        return pdfInfo;
    }

    protected Pdf2Text convertPdfToText(String paperPath) throws IOException {
        boolean usePdfBox = prop.getProperty("usePdfBox").compareToIgnoreCase("true") == 0 ? true : false;

        Pdf2Text pdf2Text = null;
        if(usePdfBox) {
            String filename = new StringBuilder()
                    .append(Config.WORK_DIR)
                    .append(prop.getProperty("papersDirectory"))
                    .append(paperPath)
                    .toString();
            String outFilename = new StringBuilder()
                    .append(Config.WORK_DIR)
                    .append(prop.getProperty("intermediatesDirectory"))
                    .append("paper.txt")
                    .toString();
            try (PDDocument pdDocument = PDDocument.load(new File(filename))) {
	            AccessPermission accessPermission = pdDocument.getCurrentAccessPermission();
	            if (!accessPermission.canExtractContent())
	                throw new IOException( "You do not have permission to extract text" );
	
	            PDFTextStripper pdfTextStripper = new PDFTextStripper();
	            //stripper.setLineSeparator(" ");
	            pdfTextStripper.setSortByPosition(false); // Keep to false, otherwise combines columns
	            pdfTextStripper.setShouldSeparateByBeads(true); // Doesn't make much difference
	            pdfTextStripper.setStartPage(1);
	            pdfTextStripper.setEndPage(Integer.MAX_VALUE);
	
	            String text = "";
	            try (Writer writer = new StringWriter()) {
		            pdfTextStripper.writeText(pdDocument, writer);
		            writeEmbedded(pdDocument, pdfTextStripper, writer);
		            text = writer.toString();
	            }

	            try (Writer fileWriter = new BufferedWriter(
	                    Utf8.newOutputStreamWriter(new FileOutputStream(outFilename)))) {
	                fileWriter.write(text);
	            }
            }

            pdf2Text = new Pdf2Text();
            pdf2Text.loadText(outFilename.toString());
        } else {
            String commandBuilder = new StringBuilder()
                    .append(prop.getProperty("pdfToText"))
                    .append(" ")
                    .append(" -enc UTF-8")
                    .append(" ")
                    .append(prop.getProperty("papersDirectory"))
                    .append(paperPath)
                    .append(" ")
                    .append(prop.getProperty("intermediatesDirectory"))
                    .append("paper.txt")
                    .toString();
            CommandLine command = CommandLine.parse(commandBuilder);
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(new File(Config.WORK_DIR));
            int exitValue = executor.execute(command);

            if (exitValue == 0) {
                pdf2Text = new Pdf2Text();
                String filename = new StringBuilder()
                        .append(Config.WORK_DIR)
                        .append(prop.getProperty("intermediatesDirectory"))
                        .append("paper.txt")
                        .toString();
                pdf2Text.loadText(filename);
            }
        }
        return pdf2Text;
    }

    private void writeEmbedded(PDDocument pdDocument, PDFTextStripper stripper, Writer writer) throws IOException {
        PDDocumentCatalog pdDocumentCatalog = pdDocument.getDocumentCatalog();

        PDDocumentNameDictionary names = pdDocumentCatalog.getNames();
        if (names == null)
            return;

        PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();
        if (embeddedFiles == null)
            return;

        Map<String, PDComplexFileSpecification> embeddedFileNames = embeddedFiles.getNames();
        if (embeddedFileNames == null)
            return;

        for (Map.Entry<String, PDComplexFileSpecification> entry: embeddedFileNames.entrySet()) {
            PDComplexFileSpecification fileSpecification = entry.getValue();
            PDEmbeddedFile embeddedFile = fileSpecification.getEmbeddedFile();

            if (embeddedFile == null || !"application/pdf".equals(embeddedFile.getSubtype()))
                continue;
            try (InputStream inputStream = embeddedFile.createInputStream()) {
                try (PDDocument subDocument = PDDocument.load(inputStream)) {
                    stripper.writeText(subDocument, writer);
                    subDocument.close();
                }
                inputStream.close();
            }
        }
    }

    protected Pdf2Xml convertPdfToXml(String paperPath) throws JAXBException, IOException, XMLStreamException {
        String commandBuilder = new StringBuilder()
                .append(prop.getProperty("pdfToHtml"))
                .append(" ")
                .append(prop.getProperty("pdfToHtmlOptions"))
                .append(" ")
                .append(prop.getProperty("papersDirectory"))
                .append(paperPath)
                .append(" ")
                .append(prop.getProperty("intermediatesDirectory"))
                .append("paper.xml")
                .toString();
        CommandLine command = CommandLine.parse(commandBuilder);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(new File(Config.WORK_DIR));
        int exitValue = executor.execute(command);

        Pdf2Xml pdf2Xml = null;
        if(exitValue == 0) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Pdf2Xml.class);

            String xml10pattern = "[^"
                    + "\u0009\r\n"
                    + "\u0020-\uD7FF"
                    + "\uE000-\uFFFD"
                    + "\ud800\udc00-\udbff\udfff"
                    + "]";
            try (BufferedReader reader = new BufferedReader(Utf8.newInputStreamReader(new FileInputStream(
                    new File(Config.WORK_DIR + prop.getProperty("intermediatesDirectory") + "paper.xml"))))) {
            	try (BufferedWriter writer = new BufferedWriter(Utf8.newOutputStreamWriter(new FileOutputStream(
                        new File(Config.WORK_DIR + prop.getProperty("intermediatesDirectory") + "paper_santized.xml"))))) {
		            String line;
		            while ((line = reader.readLine()) != null) {
		                String santizedLine = line.replaceAll(xml10pattern, "").
		                        replaceAll("((\\<i\\>)|(\\<\\/i\\>)|(\\<b\\>)|(\\<\\/b\\>))", "");
		                writer.write(santizedLine + "\n");
		            }
            	}
            }

            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader xsr = Utf8.newXMLStreamReader(xif,
                    new FileInputStream(Config.WORK_DIR + prop.getProperty("intermediatesDirectory") + "paper_santized.xml"));

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            pdf2Xml = (Pdf2Xml) unmarshaller.unmarshal(xsr);
        }

        return pdf2Xml;
    }

	@Override
	public void close() throws Exception {
		shutdown();
	}
}
