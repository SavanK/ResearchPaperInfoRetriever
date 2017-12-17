package com.repeatability.extractor.matchmakers.similaritymatcher;

import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.matchmakers.Matcher;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.*;

/**
 * Created by savan on 6/9/17.
 */
public class EmailPermutermIndex implements Matcher {

    private static final String EMAIL = "email";
//    private PaperInfo paperInfo;

    @Override
    public void setPaperInfo(PaperInfo paperInfo) {
//        this.paperInfo = paperInfo;
    }

    @Override
    public Map<String, List<PaperInfo.Match>> matchByDescPreference(List<String> args, List<String> args2) {
        List<String> emails = args;
        List<String> authors = args2;
        Map<String, List<PaperInfo.Match>> matches = new HashMap<String, List<PaperInfo.Match>>();

        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer);

        try (IndexWriter indexWriter = new IndexWriter(index, config)) {
            for (String email : emails) {
                Document document = new Document();
                document.add(new TextField(EMAIL, email.trim().toLowerCase(), Field.Store.YES));
                indexWriter.addDocument(document);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            IndexReader indexReader = DirectoryReader.open(index);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);

            for (String author : authors) {
                matches.put(author, new ArrayList<PaperInfo.Match>());

                SplitName splitName = new SplitName(author, true);

                List<WildcardQuery> wildcardQueries = constructHighConfidenceWQs(splitName);
                for (WildcardQuery wildcardQuery : wildcardQueries) {
                    TopDocs topDocs = indexSearcher.search(wildcardQuery, 10);
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        if(containsEmail(matches.get(author), emails.get(scoreDoc.doc))) {
                            PaperInfo.Match match = getMatch(matches.get(author), emails.get(scoreDoc.doc));
                            if(match.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_HIGH &&
                                    match.getConfidenceScore() < scoreDoc.score) {
                                // update score only
                                match.setConfidenceScore(scoreDoc.score);
                            }
                        } else {
                            // add new entry
                            matches.get(author).add(new PaperInfo.MatchedEmail(emails.get(scoreDoc.doc), PaperInfo.Match.CONFIDENCE_HIGH, scoreDoc.score));
                        }
                    }
                }

                wildcardQueries = constructModerateConfidenceWQs(splitName);
                for (WildcardQuery wildcardQuery : wildcardQueries) {
                    TopDocs topDocs = indexSearcher.search(wildcardQuery, 10);
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        if(containsEmail(matches.get(author), emails.get(scoreDoc.doc))) {
                            PaperInfo.Match match = getMatch(matches.get(author), emails.get(scoreDoc.doc));
                            if(match.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_MODERATE &&
                                    match.getConfidenceScore() < scoreDoc.score) {
                                // update score only
                                match.setConfidenceScore(scoreDoc.score);
                            }
                        } else {
                            // add new entry
                            matches.get(author).add(new PaperInfo.MatchedEmail(emails.get(scoreDoc.doc), PaperInfo.Match.CONFIDENCE_MODERATE, scoreDoc.score));
                        }
                    }
                }

                addMissingEmailsToEnd(matches.get(author), emails);
                if(matches.get(author).size() > 0) {
                    sortDescending(matches.get(author));
                } else {
                    matches.remove(author);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matches;
    }

    private boolean containsEmail(List<PaperInfo.Match> matches, String matchedEmail) {
        boolean contains = false;
        for (PaperInfo.Match match : matches) {
            PaperInfo.MatchedEmail email = (PaperInfo.MatchedEmail) match;
            if(matchedEmail.compareToIgnoreCase(email.getEmail()) == 0) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    private PaperInfo.Match getMatch(List<PaperInfo.Match> matches, String matchedEmail) {
        PaperInfo.Match emailMatch = null;
        for (PaperInfo.Match match : matches) {
            PaperInfo.MatchedEmail email = (PaperInfo.MatchedEmail) match;
            if(matchedEmail.compareToIgnoreCase(email.getEmail()) == 0) {
                emailMatch = match;
                break;
            }
        }
        return emailMatch;
    }

    private void addMissingEmailsToEnd(List<PaperInfo.Match> matches, List<String> emails) {
        for (String email : emails) {
            if(!containsEmail(matches, email)) {
                matches.add(new PaperInfo.MatchedEmail(email, -1, 0.f));
            }
        }
    }

    private void sortDescending(List<PaperInfo.Match> matches) {
        Collections.sort(matches, new Comparator<PaperInfo.Match>() {
            @Override
            public int compare(PaperInfo.Match m1, PaperInfo.Match m2) {
                // sort descending
                switch (m1.getConfidenceLevel()) {
                    case PaperInfo.Match.CONFIDENCE_HIGH:
                        if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_HIGH) {
                            if(m2.getConfidenceScore() < m1.getConfidenceScore()) {
                                return -1;
                            } else if(m2.getConfidenceScore() > m1.getConfidenceScore()) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_MODERATE) {
                            return -1;
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_LOW) {
                            return -1;
                        } else {
                            return -1;
                        }

                    case PaperInfo.Match.CONFIDENCE_MODERATE:
                        if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_HIGH) {
                            return 1;
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_MODERATE) {
                            if(m2.getConfidenceScore() < m1.getConfidenceScore()) {
                                return -1;
                            } else if(m2.getConfidenceScore() > m1.getConfidenceScore()) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_LOW) {
                            return -1;
                        } else {
                            return -1;
                        }

                    case PaperInfo.Match.CONFIDENCE_LOW:
                        if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_HIGH) {
                            return 1;
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_MODERATE) {
                            return 1;
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_LOW) {
                            if(m2.getConfidenceScore() < m1.getConfidenceScore()) {
                                return -1;
                            } else if(m2.getConfidenceScore() > m1.getConfidenceScore()) {
                                return 1;
                            } else {
                                return 0;
                            }
                        } else {
                            return -1;
                        }

                    default:
                        if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_HIGH) {
                            return 1;
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_MODERATE) {
                            return 1;
                        } else if(m2.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_LOW) {
                            return 1;
                        } else {
                            return 0;
                        }
                }
            }
        });
    }

//    @SuppressWarnings("Since15")
    private List<WildcardQuery> constructHighConfidenceWQs(SplitName splitName) {
        List<WildcardQuery> wildcardQueries = new ArrayList<WildcardQuery>();

        /**
         * Atleast matches either first/last name completely (High confidence)
         */
        // Permutations of first name
        if(!splitName.firstName.isEmpty()) {
            // F ; F? ; F??
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.firstName)));
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.firstName + "?")));
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.firstName + "??")));
        }

        // Permutations of last name
        if(!splitName.lastName.isEmpty()) {
            // L ; L? ; L??
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.lastName)));
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.lastName + "?")));
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.lastName + "??")));
        }

        // Permutations of first & last names
        if(!splitName.firstName.isEmpty() && !splitName.lastName.isEmpty()) {
            /* Full names */
            // F*L* ; L*F*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName + "*")));

            /* Initials only */
            // lf ; fl
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.lastName.charAt(0) + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.firstName.charAt(0) + splitName.lastName.charAt(0))));

            /* Names with initials */
            // lF* ; l?F* ; l??F**
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "?" + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "??" + splitName.firstName + "*")));

            // fL* ; f?L* ; f??L*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "??" + splitName.lastName + "*")));

            // L*f ; L*f? ; L*f??
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName.charAt(0) + "??")));

            // F*l ; F*l? ; F*l??
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName.charAt(0) + "??")));
        }

        // Permutations of first, last & middle names
        if(!splitName.firstName.isEmpty() && !splitName.lastName.isEmpty() && !splitName.middleName.isEmpty()) {
            /* Full names */
            // F*M*L* ; L*M*F*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName + "*" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName + "*" + splitName.firstName + "*")));

            /* Initials only */
            // lmf ; fml
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.lastName.charAt(0) + splitName.middleName.charAt(0) + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + splitName.lastName.charAt(0))));

            /* Names with initials */
            // F*mL* ; F*m?L* ; F*m??L* ; L*mF* ; L*m?F* ; L*m??F*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName.charAt(0) + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName.charAt(0) + "?" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName.charAt(0) + "??" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName.charAt(0) + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName.charAt(0) + "?" + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName.charAt(0) + "??" + splitName.firstName + "*")));

            // fmL* ; f?mL* ; fm?L* ; f?m?L*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.middleName.charAt(0) + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + "?" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.middleName.charAt(0) + "?" + splitName.lastName + "*")));

            // mlF* ; m?lF* ; ml?F* ; m?l?F*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.middleName.charAt(0) + splitName.lastName.charAt(0) + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0) + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.middleName.charAt(0) + splitName.lastName.charAt(0) + "?" + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0) + "?" + splitName.firstName + "*")));

            // F*ml* ; F*m?l* ; F*lm* ; F*l?m*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName.charAt(0) + splitName.lastName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName.charAt(0) + splitName.middleName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName.charAt(0) + "?" + splitName.middleName.charAt(0) + "*")));

            // L*mf* ; L*m?f* ; L*fm* ; L*f?m*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName.charAt(0) + splitName.firstName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName.charAt(0) + "?" + splitName.firstName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName.charAt(0) + "?" + splitName.middleName.charAt(0) + "*")));
        }

        return wildcardQueries;
    }

//    @SuppressWarnings("Since15")
    private List<WildcardQuery> constructModerateConfidenceWQs(SplitName splitName) {
        List<WildcardQuery> wildcardQueries = new ArrayList<WildcardQuery>();

        /**
         * Atleast matches either first/last name completely followed by additional characters (Moderate confidence)
         */
        if(!splitName.firstName.isEmpty()) {
            // F*
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.firstName + "*")));

            /* Partial names */
            // *f'*
            if(splitName.firstName.length() >= 4)
                wildcardQueries.add(new WildcardQuery(new Term(EMAIL, "*" + splitName.firstName.substring(0, 4) + "*")));
        }

        if(!splitName.lastName.isEmpty()) {
            // L*
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.lastName + "*")));

            /* Partial names */
            // *l'*
            if(splitName.lastName.length() >= 4)
                wildcardQueries.add(new WildcardQuery(new Term(EMAIL, "*" + splitName.lastName.substring(0, 4) + "*")));
        }

        if(!splitName.middleName.isEmpty() && splitName.middleName.length() >= 4) {
            // iff middle name isn't just an initial
            // M*
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, splitName.middleName + "*")));

            /* Partial names */
            // *m'*
            wildcardQueries.add(new WildcardQuery(new Term(EMAIL, "*" + splitName.middleName.substring(0, 4) + "*")));
        }

        if(!splitName.firstName.isEmpty() && !splitName.lastName.isEmpty()) {
            /* Part full names with possible partial names */
            // f*L* ; L*f* ; l*F* ; F*l*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "*" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.firstName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "*" + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.lastName.charAt(0) + "*")));

            /* Initials only */
            // f?l? ; f?l ; fl? ; fl??
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.lastName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.lastName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.firstName.charAt(0) + splitName.lastName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.firstName.charAt(0) + splitName.lastName.charAt(0) + "??")));

            // l?f? ; l?f ; lf? ; lf??
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "?" + splitName.firstName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "?" + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.lastName.charAt(0) + splitName.firstName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, "" + splitName.lastName.charAt(0) + splitName.firstName.charAt(0) + "??")));
        }

        if(!splitName.firstName.isEmpty() && !splitName.lastName.isEmpty() && !splitName.middleName.isEmpty()) {
            /* Part full names with possible partial names */
            // f*m*L* ; L*m*f* ; l*m*F* ; F*m*l*
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "*" + splitName.middleName.charAt(0) + "*" + splitName.lastName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName + "*" + splitName.middleName.charAt(0) + "*" + splitName.firstName.charAt(0) + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "*" + splitName.middleName.charAt(0) + "*" + splitName.firstName + "*")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName + "*" + splitName.middleName.charAt(0) + "*" + splitName.lastName.charAt(0) + "*")));

            /* Initials only */
            // f?ml ; f?m?l ; f?m?l? ; fm?l ; fm?l? ; fml? ; fml??
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.middleName.charAt(0) + splitName.lastName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + "?" + splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + "?" + splitName.lastName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + splitName.lastName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.firstName.charAt(0) + splitName.middleName.charAt(0) + splitName.lastName.charAt(0) + "??")));

            // l?mf ; l?m?f ; l?m?f? ; lm?f ; lm?f? ; lmf? ; lmf??
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "?" + splitName.middleName.charAt(0) + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "?" + splitName.middleName.charAt(0) + "?" + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + "?" + splitName.middleName.charAt(0) + "?" + splitName.firstName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + splitName.middleName.charAt(0) + "?" + splitName.firstName.charAt(0))));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + splitName.middleName.charAt(0) + "?" + splitName.firstName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + splitName.middleName.charAt(0) + splitName.firstName.charAt(0) + "?")));
            wildcardQueries.add(new WildcardQuery(
                    new Term(EMAIL, splitName.lastName.charAt(0) + splitName.middleName.charAt(0) + splitName.firstName.charAt(0) + "??")));
        }
        return wildcardQueries;
    }
}
