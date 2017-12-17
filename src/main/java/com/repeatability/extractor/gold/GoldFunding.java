package com.repeatability.extractor.gold;

public class GoldFunding {
    private String funding;
    private String nsfFunding;

    public GoldFunding(String funding, String nsfFunding) {
        this.funding = funding;
        this.nsfFunding = nsfFunding;
    }

    public String getFunding() {
        return funding;
    }

//    public String getNsfFunding() {
//        return nsfFunding;
//    }

    @Override
    public String toString() {
        return "Funding{" +
                "funding='" + funding + '\'' +
                ", nsfFunding='" + nsfFunding + '\'' +
                '}';
    }
}
