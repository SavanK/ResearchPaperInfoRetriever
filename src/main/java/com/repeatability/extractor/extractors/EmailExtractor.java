package com.repeatability.extractor.extractors;

import com.repeatability.extractor.PaperInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by savan on 6/6/17.
 */
public class EmailExtractor implements Extractor<String> {

    private static final String NORMAL_EMAIL_REGEX =
            "[a-z0-9A-Z\\.\\-\\_]+@[a-z0-9A-Z\\.\\-]+\\.+[a-z0-9A-Z\\.\\-]+[a-z0-9A-Z]";
    private static final String NORMAL_EMAIL_REGEX_WITH_CURLY_ENCAP =
            "[{(][a-z0-9A-Z\\.\\-\\_]+[})]@[a-z0-9A-Z\\.\\-]+\\.+[a-z0-9A-Z\\.\\-]+[a-z0-9A-Z]";
    private static final String GROUP_EMAIL_REGEX_WITH_CURLY_ENCAP =
            "[{(][a-z0-9A-Z\\.\\-\\_]+[*∗†]*[ ]*([,|/][ ]*[a-z0-9A-Z\\.\\-\\_]+[*∗†]*[ ]*)+[})][\\n]*@[a-z0-9A-Z\\.\\-]+[a-z0-9A-Z]";
    private static final String GROUP_EMAIL_REGEX_WITHOUT_CURLY_ENCAP =
            "[@]*[a-z0-9A-Z\\.\\-\\_]+[*∗†]*[ ]*([,|/][ ]*[a-z0-9A-Z\\.\\-\\_]+[*∗†]*[ ]*)+[\\n]*@[a-z0-9A-Z\\.\\-]+[a-z0-9A-Z]";
    private static final String NORMAL_EMAIL_WITH_NEWLINE_BEFORE_AT =
            "[\\n\\t\\ \\,][a-z0-9A-Z][a-z0-9A-Z\\.\\-\\_]+[\\n][a-z0-9A-Z\\.\\-\\_]*@[a-z0-9A-Z\\.\\-]+\\.+[a-z0-9A-Z\\.\\-]+[a-z0-9A-Z]";
    private static final String NORMAL_EMAIL_WITH_NEWLINE_AFTER_AT_1 =
            "[a-z0-9A-Z\\.\\-\\_]+@(([a-z0-9A-Z\\.\\-]*[\\n][a-z0-9A-Z\\.\\-]+)|([a-z0-9A-Z\\.\\-]+[\\n]))\\.+[a-z0-9A-Z\\.\\-]+[a-z0-9A-Z][\\n\\t\\ \\,]";
    private static final String NORMAL_EMAIL_WITH_NEWLINE_AFTER_AT_2 =
            "[a-z0-9A-Z\\.\\-\\_]+@[a-z0-9A-Z\\.\\-]+\\.+[\\n][a-z0-9A-Z\\.\\-]+[a-z0-9A-Z][\\n\\t\\ \\,]";

    @Override
    public List<String> extract(PaperInfo paperInfo) {
        List<String> emails = new ArrayList<>();
        List<String> probableEmails = new ArrayList<>();

        Pattern normalPattern = Pattern.compile(NORMAL_EMAIL_REGEX);
        Pattern normalWithEncapPattern = Pattern.compile(NORMAL_EMAIL_REGEX_WITH_CURLY_ENCAP);
        Pattern groupWithEncapPattern = Pattern.compile(GROUP_EMAIL_REGEX_WITH_CURLY_ENCAP);
        Pattern groupWithoutEncapPattern = Pattern.compile(GROUP_EMAIL_REGEX_WITHOUT_CURLY_ENCAP);
        Pattern normalWNLBeforePattern = Pattern.compile(NORMAL_EMAIL_WITH_NEWLINE_BEFORE_AT);
        Pattern normalWNLAfterPattern1 = Pattern.compile(NORMAL_EMAIL_WITH_NEWLINE_AFTER_AT_1);
        Pattern normalWNLAfterPattern2 = Pattern.compile(NORMAL_EMAIL_WITH_NEWLINE_AFTER_AT_2);

        String text = paperInfo.getPdf2Text().getText();
        Matcher normalMatcher = normalPattern.matcher(text);
        Matcher normalWithEncapMatcher = normalWithEncapPattern.matcher(text);
        Matcher groupWithEncapMatcher = groupWithEncapPattern.matcher(text);
        Matcher groupWithoutEncapMatcher = groupWithoutEncapPattern.matcher(text);
        Matcher normalWNLBeforeMatcher = normalWNLBeforePattern.matcher(text);
        Matcher normalWNLAfterMatcher1 = normalWNLAfterPattern1.matcher(text);
        Matcher normalWNLAfterMatcher2 = normalWNLAfterPattern2.matcher(text);

        while (normalMatcher.find()) {
            String email = normalMatcher.group(0);
            email = email.trim().toLowerCase();
            if(!emails.contains(email) && !skip(email))
                emails.add(email);
        }

        while (normalWithEncapMatcher.find()) {
            String email = normalWithEncapMatcher.group(0);
            email = email.trim().toLowerCase().replaceAll("[{]","").replaceAll("[}]","");
            if(!emails.contains(email) && !skip(email))
                emails.add(email);
        }

        while (groupWithEncapMatcher.find()) {
            String token = groupWithEncapMatcher.group(0).trim().replaceAll("[\n]","");
            token = removeAllSpecialChars(token);
            if(token.contains("{") && token.contains("}")) {
                String emailIds = token.substring(token.indexOf('{') + 1, token.indexOf('}'));

                String emailDomain = token.substring(token.indexOf('}') + 1);
                emailDomain.trim();

                String delimiter;
                if (emailIds.contains(",")) {
                    delimiter = "[,]";
                } else if (emailIds.contains("|")) {
                    delimiter = "[|]";
                } else if(emailIds.contains("/")) {
                    delimiter = "[/]";
                } else if(emailIds.contains(";")) {
                    delimiter = "[;]";
                } else {
                    delimiter = "[,]";
                }

                String[] emailIdList = emailIds.split(delimiter);
                for (String emailId : emailIdList) {
                    StringBuilder email = new StringBuilder();

                    email.append(emailId.trim());
                    email.append(emailDomain);
                    String emailStr = email.toString().toLowerCase();
                    if (!emails.contains(emailStr) && !skip(emailStr)) {
                        emails.add(emailStr);
                    }
                }
            }
        }

        while (groupWithoutEncapMatcher.find()) {
            String token = groupWithoutEncapMatcher.group(0).trim().replaceAll("[\n]","");
            token = removeAllSpecialChars(token);
            if(token.startsWith("@")) {
                // Ignoring false positives. Like,
                // maojj12@mails.tsinghua.edu.cn, objectkuan@gmail.com, weixu@tsinghua.edu.cn, shiyc@tsinghua.edu.cn
                break;
            }
            // Considering true positives. Like,
            // abc, def @gmail.com

            String emailIds = token.substring(0, token.indexOf('@'));
            String emailDomain = token.substring(token.indexOf('@'));
            emailDomain.trim();
            emailIds.trim();

            String delimiter;
            if (emailIds.contains(",")) {
                delimiter = "[,]";
            } else if (emailIds.contains("|")) {
                delimiter = "[|]";
            } else if(emailIds.contains("/")) {
                delimiter = "[/]";
            } else if(emailIds.contains(";")) {
                delimiter = "[;]";
            } else {
                delimiter = "[,]";
            }

            String[] emailIdList = emailIds.split(delimiter);
            for (String emailId : emailIdList) {
                StringBuilder email = new StringBuilder();

                email.append(emailId.trim());
                email.append(emailDomain);
                String emailStr = email.toString().toLowerCase();
                if (!emails.contains(emailStr) && !skip(emailStr)) {
                    emails.add(emailStr);
                }
            }
        }

        while (normalWNLBeforeMatcher.find()) {
            String pEmail = normalWNLBeforeMatcher.group(0).substring(1).replace("\n","");
            if(!skip(pEmail)) {
                boolean valid = true;
                for (String email : emails) {
                    if (pEmail.endsWith(email)) {
                        valid = false;
                        break;
                    }
                }
                if(valid)
                    probableEmails.add(pEmail);
            }
        }

        while (normalWNLAfterMatcher1.find()) {
            String pEmail = normalWNLAfterMatcher1.group(0);
            pEmail = pEmail.substring(0, pEmail.length()-1).replace("\n","");
            if(!skip(pEmail)) {
                boolean valid = true;
                for (String email : emails) {
                    if (pEmail.startsWith(email)) {
                        valid = false;
                        break;
                    }
                }
                if(valid)
                    probableEmails.add(pEmail);
            }
        }

        while (normalWNLAfterMatcher2.find()) {
            String pEmail = normalWNLAfterMatcher2.group(0);
            pEmail = pEmail.substring(0, pEmail.length()-1).replace("\n","");
            if(!skip(pEmail)) {
                boolean valid = true;
                for (String email : emails) {
                    if (pEmail.startsWith(email)) {
                        valid = false;
                        break;
                    }
                }
                if(valid)
                    probableEmails.add(pEmail);
            }
        }

        emails.addAll(probableEmails);
        return emails;
    }

    private boolean skip(String email) {
        boolean retVal = false;
        if(email.toLowerCase().contains("permissions@acm.org")) {
            retVal = true;
        }
        return retVal;
    }

    private String removeAllSpecialChars(String text) {
        String nonLatinUnicodeChars = "[^"
                + "\u0020-\u007F"
                + "\u00c0-\u00ff"
                + "\u0100-\u017f"
                + "]";
        return text.replaceAll(nonLatinUnicodeChars, "");
    }
}
