----Repeatability Extractor----

1. Compilation
   a. Use Maven to compile
      	mvn compile

2. Execution
   a. Use Maven to execute the example program which uses the Repeatability Extractor library
	mvn exec:java
	  
   b. Typical App prompt looks like this:
   
      paper_1.pdf
      Adversarial patrolling with spatially uncertain alarm signals
            Nicola Basilico : MatchedEmail{fundings='nicola.basilico@unimi.it', confidenceLevel=HIGH, confidenceScore=0.95324075}
	    Giuseppe De Nittis : MatchedEmail{fundings='XXXXXXXXXX1@XXXXXXXXXX', confidenceLevel=NONE, confidenceScore=0.0}
	    Nicola Gatti : MatchedEmail{fundings='XXXXXXXXXX2@XXXXXXXXXX', confidenceLevel=NONE, confidenceScore=0.0}
      paper_2.pdf
      An adaptive trust-Stackelberg game model for security and energy efficiency in dynamic cognitive radio networks
            Kim-Kwang Raymond Choo : MatchedEmail{fundings='XXXXXXXXXX3@XXXXXXXXXX', confidenceLevel=NONE, confidenceScore=0.0}
	    He Fang : MatchedEmail{fundings='XXXXXXXXXX1@XXXXXXXXXX', confidenceLevel=NONE, confidenceScore=0.0}
	    Jie Li : MatchedEmail{fundings='XXXXXXXXXX2@XXXXXXXXXX', confidenceLevel=NONE, confidenceScore=0.0}
	    Li Xu : MatchedEmail{fundings='xuli@fjnu.edu.cn', confidenceLevel=HIGH, confidenceScore=1.0}

3. Important path information
   a. Working dir: src/main/resources
   b. Papers dir: <Working dir>/papers
      [Considers files mentioned in the "gold_result.csv" file only]
   c. Intermediates dir: <Working dir>/intermediates
   d. Output dir: <Working dir>/out
   e. Gold dir: <Working dir>/gold