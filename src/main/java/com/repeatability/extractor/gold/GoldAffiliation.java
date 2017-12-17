package com.repeatability.extractor.gold;

public class GoldAffiliation {
    private String institution;
    private String department;

    public GoldAffiliation(String institution, String department) {
        this.institution = institution == null ? "" : institution;
        this.department = department == null ? "" : department;
    }

    public String getInstitution() {
        return institution;
    }

//    public String getDepartment() {
//        return department;
//    }

    @Override
    public String toString() {
        return "Affiliations{" + institution + "," + department + "}";
    }
}
