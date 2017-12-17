package com.repeatability.extractor;

import com.repeatability.extractor.pdfparsers.pdf2text.Pdf2Text;
import com.repeatability.extractor.pdfparsers.pdf2xml.Pdf2Xml;
import com.repeatability.extractor.pdfparsers.pdfinfo.PdfInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by savan on 5/24/17.
 */
public class PaperInfo {
    private String paperPath;
    private List<String> authors;
    private String key;

    private PdfInfo pdfInfo;
    private Pdf2Text pdf2Text;
    private Pdf2Xml pdf2Xml;

    private Map<String, MatchedEmail> authorEmails;
    private Map<String, MatchedAffiliation> authorAffiliations;
    private List<MatchedFunding> fundings;
    private List<MatchedArtifact> artifacts;

    public PaperInfo(String paperPath, String key, List<String> authors) {
        this.paperPath = paperPath;
        this.authors = authors;
        this.key = key;

        authorAffiliations = new HashMap<>();
        authorEmails = new HashMap<>();
    }

    public PdfInfo getPdfInfo() {
        return pdfInfo;
    }

    public void setPdfInfo(PdfInfo pdfInfo) {
        this.pdfInfo = pdfInfo;
    }

    public Pdf2Text getPdf2Text() {
        return pdf2Text;
    }

    public void setPdf2Text(Pdf2Text pdf2Text) {
        this.pdf2Text = pdf2Text;
    }

    public Pdf2Xml getPdf2Xml() {
        return pdf2Xml;
    }

    public void setPdf2Xml(Pdf2Xml pdf2Xml) {
        this.pdf2Xml = pdf2Xml;
    }

    public Map<String, MatchedEmail> getAuthorEmails() {
        return authorEmails;
    }

    public void setAuthorEmails(Map<String, MatchedEmail> authorEmails) {
        this.authorEmails = authorEmails;
    }

    public Map<String, MatchedAffiliation> getAuthorAffiliations() {
        return authorAffiliations;
    }

    public void setAuthorAffiliations(Map<String, MatchedAffiliation> authorAffiliations) {
        this.authorAffiliations = authorAffiliations;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getKey() {
        return key;
    }

    public String getPaperPath() {
        return paperPath;
    }

    public List<MatchedFunding> getFundings() {
        return fundings;
    }

    public void setFundings(List<MatchedFunding> fundings) {
        this.fundings = fundings;
    }

    public List<MatchedArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<MatchedArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public interface Match {
        int CONFIDENCE_HIGH = 0;
        int CONFIDENCE_MODERATE = 1;
        int CONFIDENCE_LOW = 2;

        int getConfidenceLevel();
        float getConfidenceScore();
        void setConfidenceLevel(int level);
        void setConfidenceScore(float score);
    }

    public static class MatchedFunding implements Match {
        private String fundingInfo;
        private Set<String> fundingOrgs;
        private Set<String> grantIds;
        private int confidenceLevel;
        private float confidenceScore;

        public MatchedFunding(String fundingInfo, Set<String> fundingOrgs, Set<String> grantIds, int confidenceLevel, float confidenceScore) {
            this.confidenceLevel = confidenceLevel;
            this.confidenceScore = confidenceScore;
            this.fundingInfo = fundingInfo;
            this.fundingOrgs = fundingOrgs;
            this.grantIds = grantIds;
        }

        @Override
        public int getConfidenceLevel() {
            return confidenceLevel;
        }

        @Override
        public float getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public void setConfidenceLevel(int level) {
            confidenceLevel = level;
        }

        @Override
        public void setConfidenceScore(float score) {
            confidenceScore = score;
        }

        public String getFundingInfo() {
            return fundingInfo;
        }

        public Set<String> getFundingOrgs() {
            return fundingOrgs;
        }

        public Set<String> getGrantIds() {
            return grantIds;
        }

        @Override
        public boolean equals(Object obj) {
            MatchedFunding matchedFunding = (MatchedFunding) obj;
            return fundingInfo == matchedFunding.getFundingInfo() &&
                    fundingOrgs == matchedFunding.getFundingOrgs() &&
                    grantIds == matchedFunding.getGrantIds() &&
                    confidenceLevel == matchedFunding.getConfidenceLevel() &&
                    confidenceScore == matchedFunding.getConfidenceScore();
        }

        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer
                    .append("MatchedFunding{")
                    .append("confidenceLevel=")
                    .append(confidenceLevel == CONFIDENCE_HIGH ? "HIGH" :
                            confidenceLevel == CONFIDENCE_MODERATE ? "MODERATE" :
                            confidenceLevel == CONFIDENCE_LOW ? "LOW" : "NONE")
                    .append(", confidenceScore=")
                    .append(confidenceScore)
                    .append("\n\t\tFunding info={")
                    .append(fundingInfo)
                    .append("}")
                    .append("\n\t\tFunding organizations={");
            for (String organization : fundingOrgs) {
                stringBuffer.append(organization + ", ");
            }
            stringBuffer
                    .append("}")
                    .append("\n\t\tGrant IDs={");
            for (String grantId : grantIds) {
                stringBuffer.append(grantId + ", ");
            }
            stringBuffer.append("}}");
            return stringBuffer.toString();
        }
    }

    public static class MatchedArtifact implements Match {
        private String artifactInfo;
        private String artifactLink;
        private int confidenceLevel;
        private float confidenceScore;

        public MatchedArtifact(String artifactInfo, String artifactLink, int confidenceLevel, float confidenceScore) {
            this.confidenceLevel = confidenceLevel;
            this.confidenceScore = confidenceScore;
            this.artifactInfo = artifactInfo;
            this.artifactLink = artifactLink;
        }

        @Override
        public int getConfidenceLevel() {
            return confidenceLevel;
        }

        @Override
        public float getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public void setConfidenceLevel(int level) {
            confidenceLevel = level;
        }

        @Override
        public void setConfidenceScore(float score) {
            confidenceScore = score;
        }

        public String getArtifactInfo() {
            return artifactInfo;
        }

        public String getArtifactLink() {
            return artifactLink;
        }

        @Override
        public boolean equals(Object obj) {
            MatchedArtifact matchedArtifact = (MatchedArtifact) obj;
            return artifactInfo == matchedArtifact.getArtifactInfo() &&
                    artifactLink == matchedArtifact.getArtifactLink() &&
                    confidenceLevel == matchedArtifact.getConfidenceLevel() &&
                    confidenceScore == matchedArtifact.getConfidenceScore();
        }

        @Override
        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("MatchedArtifact{");
            stringBuffer.append("confidenceLevel=" +
                    (confidenceLevel == CONFIDENCE_HIGH ?
                            "HIGH" :
                            confidenceLevel == CONFIDENCE_MODERATE ? "MODERATE" :
                                    confidenceLevel == CONFIDENCE_LOW ? "LOW" : "NONE") +
                    ", confidenceScore=" + confidenceScore);
            stringBuffer.append("\n\t\tArtifact info={");
            stringBuffer.append(artifactInfo);
            stringBuffer.append("}");
            stringBuffer.append("\n\t\tArtifact link={" + artifactLink);
            stringBuffer.append("}}");
            return stringBuffer.toString();
        }
    }

    public static class MatchedEmail implements Match {

        private String email;
        private int confidenceLevel;
        private float confidenceScore;

        public MatchedEmail(String email, int confidenceLevel, float confidenceScore) {
            this.confidenceLevel = confidenceLevel;
            this.confidenceScore = confidenceScore;
            this.email = email;
        }

        @Override
        public int getConfidenceLevel() {
            return confidenceLevel;
        }

        @Override
        public float getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public void setConfidenceLevel(int level) {
            confidenceLevel = level;
        }

        @Override
        public void setConfidenceScore(float score) {
            confidenceScore = score;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public boolean equals(Object obj) {
            MatchedEmail matchedEmail = (MatchedEmail) obj;
            return email.compareToIgnoreCase(matchedEmail.getEmail()) == 0 &&
                    confidenceLevel == matchedEmail.getConfidenceLevel() &&
                    confidenceScore == matchedEmail.getConfidenceScore();
        }

        @Override
        public String toString() {
            return "MatchedEmail{" +
                    "email='" + email + '\'' +
                    ", confidenceLevel=" +
                    (confidenceLevel == CONFIDENCE_HIGH ?
                            "HIGH" :
                            confidenceLevel == CONFIDENCE_MODERATE ? "MODERATE" :
                                    confidenceLevel == CONFIDENCE_LOW ? "LOW" : "NONE") +
                    ", confidenceScore=" + confidenceScore +
                    '}';
        }
    }

    public static class MatchedAffiliation implements Match {

        private String affiliation;
        private int confidenceLevel;
        private float confidenceScore;

        public MatchedAffiliation(String affiliation, int confidenceLevel, float confidenceScore) {
            this.confidenceLevel = confidenceLevel;
            this.confidenceScore = confidenceScore;
            this.affiliation = affiliation;
        }

        @Override
        public int getConfidenceLevel() {
            return confidenceLevel;
        }

        @Override
        public float getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public void setConfidenceLevel(int level) {
            confidenceLevel = level;
        }

        @Override
        public void setConfidenceScore(float score) {
            confidenceScore = score;
        }

        public String getAffiliation() {
            return affiliation;
        }

        @Override
        public boolean equals(Object obj) {
            MatchedAffiliation matchedEmail = (MatchedAffiliation) obj;
            return affiliation.compareToIgnoreCase(matchedEmail.getAffiliation()) == 0 &&
                    confidenceLevel == matchedEmail.getConfidenceLevel() &&
                    confidenceScore == matchedEmail.getConfidenceScore();
        }

        @Override
        public String toString() {
            return "MatchedAffiliation{" +
                    "affiliation='" + affiliation + '\'' +
                    ", confidenceLevel=" +
                    (confidenceLevel == CONFIDENCE_HIGH ?
                            "HIGH" :
                            confidenceLevel == CONFIDENCE_MODERATE ? "MODERATE" :
                                    confidenceLevel == CONFIDENCE_LOW ? "LOW" : "NONE") +
                    ", confidenceScore=" + confidenceScore +
                    '}';
        }
    }

    public static class MatchedAuthor implements Match {

        private String author;
        private int confidenceLevel;
        private float confidenceScore;

        public MatchedAuthor(String author, int confidenceLevel, float confidenceScore) {
            this.confidenceLevel = confidenceLevel;
            this.confidenceScore = confidenceScore;
            this.author = author;
        }

        @Override
        public int getConfidenceLevel() {
            return confidenceLevel;
        }

        @Override
        public float getConfidenceScore() {
            return confidenceScore;
        }

        @Override
        public void setConfidenceLevel(int level) {
            confidenceLevel = level;
        }

        @Override
        public void setConfidenceScore(float score) {
            confidenceScore = score;
        }

        public String getAuthor() {
            return author;
        }

        @Override
        public boolean equals(Object obj) {
            MatchedAuthor matchedAuthor = (MatchedAuthor) obj;
            return author.compareToIgnoreCase(matchedAuthor.getAuthor()) == 0 &&
                    confidenceLevel == matchedAuthor.getConfidenceLevel() &&
                    confidenceScore == matchedAuthor.getConfidenceScore();
        }

        @Override
        public String toString() {
            return "MatchedAuthor{" +
                    "author='" + author + '\'' +
                    ", confidenceLevel=" +
                    (confidenceLevel == CONFIDENCE_HIGH ?
                            "HIGH" :
                            confidenceLevel == CONFIDENCE_MODERATE ? "MODERATE" :
                                    confidenceLevel == CONFIDENCE_LOW ? "LOW" : "NONE") +
                    ", confidenceScore=" + confidenceScore +
                    '}';
        }
    }

}
