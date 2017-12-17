package com.repeatability.extractor.gold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GoldPaper {
    private static final String PATH_SUFFIX = "/paper.pdf";

    public static String getPaperPath(String key) {
    	return key + PATH_SUFFIX;
    }
    
    private Map<String, String> authorEmails;
    private Map<String, GoldAffiliation> authorAffiliations;
    private Set<String> artifacts;
    private String key;
    private String paperPath;
    private GoldFunding funding;

    public GoldPaper(String key) {
        this.key = key;
        this.paperPath = getPaperPath(key);
        authorEmails = new HashMap<>();
        artifacts = new HashSet<>();
        authorAffiliations = new HashMap<>();
    }

    public void addAuthorAffiliation(String author, GoldAffiliation affiliation) {
        authorAffiliations.put(author, affiliation);
    }

    public void addAuthorEmail(String author, String email) {
        authorEmails.put(author, email);
    }

    public void addArtifact(String artifact) {
        artifacts.add(artifact.toLowerCase().trim());
    }

    public List<String> getAuthors() {
        List<String> authors = new ArrayList<String>();
        for (String author : authorEmails.keySet()) {
            authors.add(author);
        }
        return authors;
    }

    public Set<String> getArtifacts() {
        return artifacts;
    }

    public String getEmail(String author) {
        return authorEmails.get(author);
    }

    public GoldAffiliation getAffiliation(String author) {
        return authorAffiliations.get(author);
    }

    public String getKey() {
        return key;
    }

    public String getPaperPath() {
        return paperPath;
    }

    public GoldFunding getFunding() {
        return funding;
    }

    public void setFunding(GoldFunding funding) {
        this.funding = funding;
    }
}
