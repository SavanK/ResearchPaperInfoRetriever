package com.repeatability.extractor.matchmakers.similaritymatcher;

/**
 * Created by savan on 6/10/17.
 */
public class SplitName {
    public String firstName;
    public String lastName;
    public String middleName;
    public String suffix;
    public String fullName;

//    @SuppressWarnings("Since15")
    public SplitName(String fullName, boolean removeHyphens) {
        this.fullName = fullName;
        String[] splitNames = fullName.split(" ");

        int i=0;
        for (String sName : splitNames) {
            splitNames[i] = removeHyphens ? sName.toLowerCase().replace("-","") : sName.toLowerCase();
            i++;
        }

        switch (splitNames.length) {
            case 0:
                firstName = "";
                middleName = "";
                lastName = "";
                suffix = "";
                break;

            case 1:
                lastName = splitNames[0];
                firstName = "";
                middleName = "";
                suffix = "";
                break;

            case 2:
                if(!isValidSuffix(splitNames[1])) {
                    lastName = splitNames[1];
                    firstName = splitNames[0];
                    middleName = "";
                    suffix = "";
                } else {
                    lastName = "";
                    firstName = splitNames[0];
                    middleName = "";
                    suffix = splitNames[1];
                }
                break;

            case 3:
                if(!isValidSuffix(splitNames[2])) {
                    lastName = splitNames[2];
                    firstName = splitNames[0];
                    middleName = splitNames[1];
                    suffix = "";
                } else {
                    lastName = splitNames[1];
                    firstName = splitNames[0];
                    middleName = "";
                    suffix = splitNames[2];
                }
                break;

            case 4:
                lastName = splitNames[2];
                firstName = splitNames[0];
                middleName = splitNames[1];
                suffix = splitNames[3];
                break;
        }

        if(!middleName.isEmpty()) {
            // Remove '.' if initials
            // Ex: John W. Oliver
            // Before: middle_name="W."
            // After: middle_name="W"
            if(middleName.endsWith(".")) {
                middleName = middleName.substring(0, middleName.length()-1);
            }
        }
    }

    private boolean isValidSuffix(String suffix) {
        return suffix.compareToIgnoreCase("jr.") == 0 ||
                suffix.compareToIgnoreCase("sr.") == 0 ||
                suffix.compareToIgnoreCase("jr") == 0 ||
                suffix.compareToIgnoreCase("sr") == 0 ||
                suffix.compareToIgnoreCase("I") == 0 ||
                suffix.compareToIgnoreCase("II") == 0 ||
                suffix.compareToIgnoreCase("III") == 0 ||
                suffix.compareToIgnoreCase("IV") == 0;
    }
}
