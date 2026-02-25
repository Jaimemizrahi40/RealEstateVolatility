import java.util.*;
import java.util.regex.*;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import java.io.*;

/**
* Volatility.java
*
* Full-featured market volatility analysis tool.
* Handles adding, viewing, updating, comparing, removing markets,
* and extracting rankings.
*/
public class Volatility {
    static ArrayList<Market> marketList = new ArrayList<Market>();

    public static void main(String args[]) {
        // Keep one scanner open for the full session.
        Scanner scanner = new Scanner(System.in);
        String answerMain = "";

        // Loop until the user chooses the explicit exit option.
        while (!(answerMain.toUpperCase().equals("G"))) {
            // Print the top-level menu every iteration.
            System.out.println("Select from the following choices (only write the letter):");
            System.out.println("A. Add Market");
            System.out.println("B. View Market");
            System.out.println("C. Update Market");
            System.out.println("D. Compare Markets");
            System.out.println("E. Remove Market");
            System.out.println("F. Extract Rankigs");
            System.out.println("G. Exit");
            System.out.print("Enter your choice: ");

            // Handle EOF or closed stdin cleanly.
            if (!scanner.hasNextLine()) {
                System.out.println("No input detected. Exiting.");
                break;
            }

            // Normalize casing and whitespace before routing.
            answerMain = scanner.nextLine().trim();

            // Route to "Add Market".
            if (answerMain.toUpperCase().equals("A")) {
                addMarketProcess(scanner);
            }

            // Route to "View Market".
            if (answerMain.toUpperCase().equals("B")) {
                viewMarketProcess(scanner);
            }

            // Route to "Update Market".
            if (answerMain.toUpperCase().equals("C")) {
                updateMarketProcess(scanner);
            }

            // Route to "Compare Markets".
            if (answerMain.toUpperCase().equals("D")) {
                compareMarketsProcess(scanner);
            }

            // Route to "Remove Market".
            if (answerMain.toUpperCase().equals("E")) {
                removeMarketProcess(scanner);
            }

            // Route to "Extract Rankings".
            if (answerMain.toUpperCase().equals("F")) {
                extractRankingsProcess();
            }

            // Give feedback for unsupported menu keys.
            if (!answerMain.isEmpty() && !"ABCDEFG".contains(answerMain.toUpperCase())) {
                System.out.println("Invalid choice. Please enter A, B, C, D, E, F, or G.");
            }

            System.out.println();
        }

        scanner.close();
    }

    // Adds one market, calculates metrics, updates ranking files, and appends market details.
    private static void addMarketProcess(Scanner scanner) {
        // Prompt for the market identity first.
        System.out.println("Enter the name of the market:");
        if (!scanner.hasNextLine()) {
            System.out.println("No market name provided.");
            return;
        }
        String marketName = scanner.nextLine().trim();
        if (marketName.isEmpty()) {
            System.out.println("Market name cannot be empty.");
            return;
        }

        // Read all three market metric series from the user.
        System.out.println("Enter the rent growth for past 10 years, seperated by commas:");
        ArrayList<Double> tenYrRG = parseDoubleList(scanner.nextLine());

        System.out.println("Enter the vacancy for past 10 years, seperated by commas:");
        ArrayList<Double> tenYrVac = parseDoubleList(scanner.nextLine());

        System.out.println("Enter the cap rate for past 10 years, seperated by commas:");
        ArrayList<Double> tenYrCap = parseDoubleList(scanner.nextLine());

        // Reject malformed numbers or mismatched list lengths up front.
        if (tenYrRG == null || tenYrVac == null || tenYrCap == null) {
            System.out.println("Invalid numeric input. Please enter comma-separated numbers only.");
            return;
        }
        if (tenYrRG.isEmpty() || tenYrVac.isEmpty() || tenYrCap.isEmpty()) {
            System.out.println("Each metric requires at least one numeric value.");
            return;
        }
        if (tenYrRG.size() != tenYrVac.size() || tenYrRG.size() != tenYrCap.size()) {
            System.out.println("Rent Growth, Vacancy, and Cap Rate lists must have the same number of entries.");
            return;
        }

        // Pull national benchmark series for beta calculations.
        ArrayList<ArrayList<Double>> nationalData = getNationalData();
        if (nationalData == null || nationalData.size() < 3) {
            System.out.println("National data could not be loaded.");
            return;
        }

        ArrayList<Double> tenYrNationalRG = nationalData.get(0);
        ArrayList<Double> tenYrNationalVac = nationalData.get(1);
        ArrayList<Double> tenYrNationalCap = nationalData.get(2);

        // Ensure national lists are present and aligned with market lengths.
        if (tenYrNationalRG.isEmpty() || tenYrNationalVac.isEmpty() || tenYrNationalCap.isEmpty()) {
            System.out.println("National data is missing one or more metric lists.");
            return;
        }

        if (tenYrNationalRG.size() != tenYrRG.size() || tenYrNationalVac.size() != tenYrVac.size()
                || tenYrNationalCap.size() != tenYrCap.size()) {
            System.out.println("National data length must match market data length for each metric.");
            return;
        }

        // Compute the core risk metrics for this market.
        double stdDevVac = stdDev(tenYrVac);
        double stdDevRG = stdDev(tenYrRG);
        double stdDevCap = stdDev(tenYrCap);

        System.out.println();

        // Echo calculated values so the user can sanity-check inputs.
        System.out.println("Standard Deviation Vacancy: " + stdDevVac);
        System.out.println("Standard Deviation Rent Growth: " + stdDevRG);
        System.out.println("Standard Deviation Cap Rate: " + stdDevCap);

        double cvVacancy = CV(stdDevVac, avg(tenYrVac));
        double cvRG = CV(stdDevRG, avg(tenYrRG));
        double cvCap = CV(stdDevCap, avg(tenYrCap));

        System.out.println("CV for Vacancy: " + cvVacancy);
        System.out.println("CV for Rent Growth: " + cvRG);
        System.out.println("CV for Cap Rate: " + cvCap);

        double betaVacancy = beta(tenYrNationalVac, tenYrVac);
        double betaRG = beta(tenYrNationalRG, tenYrRG);
        double betaCap = beta(tenYrNationalCap, tenYrCap);

        System.out.println("Beta for Vacancy: " + betaVacancy);
        System.out.println("Beta for Rent Growth: " + betaRG);
        System.out.println("Beta for Cap Rate: " + betaCap);

        // Initialize all ranking buckets used by Rankings.txt.
        ArrayList<String> cvVacancyList = new ArrayList<String>();
        ArrayList<String> cvRGList = new ArrayList<String>();
        ArrayList<String> cvCapList = new ArrayList<String>();
        ArrayList<String> stdDevVacList = new ArrayList<String>();
        ArrayList<String> stdDevRGList = new ArrayList<String>();
        ArrayList<String> stdDevCapList = new ArrayList<String>();
        ArrayList<String> betaVacancyList = new ArrayList<String>();
        ArrayList<String> betaRGList = new ArrayList<String>();
        ArrayList<String> betaCapList = new ArrayList<String>();

        // Load existing ranking lines if the file already exists.
        try {
            BufferedReader reader = new BufferedReader(new FileReader("Rankings.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("CV Vacancy Rankings: ")) {
                    parseRankingLine(line, "CV Vacancy Rankings: ", cvVacancyList);
                } else if (line.startsWith("CV Rent Growth Rankings: ")) {
                    parseRankingLine(line, "CV Rent Growth Rankings: ", cvRGList);
                } else if (line.startsWith("CV Cap Rate Rankings: ")) {
                    parseRankingLine(line, "CV Cap Rate Rankings: ", cvCapList);
                } else if (line.startsWith("Beta Vacancy Rankings: ")) {
                    parseRankingLine(line, "Beta Vacancy Rankings: ", betaVacancyList);
                } else if (line.startsWith("Beta Rent Growth Rankings: ")) {
                    parseRankingLine(line, "Beta Rent Growth Rankings: ", betaRGList);
                } else if (line.startsWith("Beta Cap Rate Rankings: ")) {
                    parseRankingLine(line, "Beta Cap Rate Rankings: ", betaCapList);
                } else if (line.startsWith("Standard Deviation Cap Rate Rankings: ")) {
                    parseRankingLine(line, "Standard Deviation Cap Rate Rankings: ", stdDevCapList);
                } else if (line.startsWith("Standard Deviation Rent Growth Rankings: ")) {
                    parseRankingLine(line, "Standard Deviation Rent Growth Rankings: ", stdDevRGList);
                } else if (line.startsWith("Standard Deviation Vacancy Rankings: ")) {
                    parseRankingLine(line, "Standard Deviation Vacancy Rankings: ", stdDevVacList);
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            // First run is valid; rankings file will be created below.
        } catch (IOException e) {
            System.out.println("An error occured: " + e.getMessage());
            return;
        }

        // Append this market's fresh values into each category.
        cvVacancyList.add(marketName + " - " + cvVacancy);
        cvRGList.add(marketName + " - " + cvRG);
        cvCapList.add(marketName + " - " + cvCap);
        betaVacancyList.add(marketName + " - " + betaVacancy);
        betaRGList.add(marketName + " - " + betaRG);
        betaCapList.add(marketName + " - " + betaCap);
        stdDevVacList.add(marketName + " - " + stdDevVac);
        stdDevRGList.add(marketName + " - " + stdDevRG);
        stdDevCapList.add(marketName + " - " + stdDevCap);

        // Keep all ranking lists sorted by numeric metric value.
        cvVacancyList.sort(Volatility::compareMarketValues);
        cvRGList.sort(Volatility::compareMarketValues);
        cvCapList.sort(Volatility::compareMarketValues);
        betaVacancyList.sort(Volatility::compareMarketValues);
        betaRGList.sort(Volatility::compareMarketValues);
        betaCapList.sort(Volatility::compareMarketValues);
        stdDevVacList.sort(Volatility::compareMarketValues);
        stdDevRGList.sort(Volatility::compareMarketValues);
        stdDevCapList.sort(Volatility::compareMarketValues);

        // Rewrite the ranking file in canonical order.
        writeRankingsToFile(cvVacancyList, cvRGList, cvCapList, betaVacancyList, betaRGList,
                betaCapList, stdDevVacList, stdDevRGList, stdDevCapList);

        // Persist this full market record in Markets.txt.
        Market market = new Market(marketName, stdDevVac, stdDevRG, stdDevCap, cvVacancy, cvRG, cvCap,
                betaVacancy, betaRG, betaCap, tenYrRG, tenYrCap, tenYrVac);

        writeMarketToFile(market);
    }

    // Looks up one market and prints its ranking position across all metrics.
    private static void viewMarketProcess(Scanner scanner) {
        // Ask which market to inspect.
        System.out.println("Market Name: ");
        if (!scanner.hasNextLine()) {
            System.out.println("No market name provided.");
            return;
        }
        String marketViewName = scanner.nextLine().trim();
        if (marketViewName.isEmpty()) {
            System.out.println("Market name cannot be empty.");
            return;
        }

        System.out.println();
        // Print a simple header before results.
        System.out.println("Market: " + marketViewName);
        System.out.println("____________________");

        // Resolve market details from persistent storage.
        Market market = findMarketInFile(marketViewName);

        if (market == null) {
            System.out.println("Market not found");
            return;
        }

        // Pull all ranking lists so rank labels can be displayed.
        ArrayList<ArrayList<String>> rankings = extractAllRankings();
        if (rankings == null || rankings.size() < 9) {
            System.out.println("One or more of the rankings lists are null");
            return;
        }

        ArrayList<String> stdDevCapList = rankings.get(0);
        ArrayList<String> stdDevRGList = rankings.get(1);
        ArrayList<String> stdDevVacList = rankings.get(2);
        ArrayList<String> cvCapList = rankings.get(3);
        ArrayList<String> betaCapList = rankings.get(4);
        ArrayList<String> cvRGList = rankings.get(5);
        ArrayList<String> betaRGList = rankings.get(6);
        ArrayList<String> cvVacancyList = rankings.get(7);
        ArrayList<String> betaVacancyList = rankings.get(8);

        // Print rank placement for each tracked metric.
        System.out.println("Ranking for CV Rent Growth: " + viewRanking(market.getMarketName(), cvRGList));
        System.out.println("Ranking for CV Vacancy: " + viewRanking(market.getMarketName(), cvVacancyList));
        System.out.println("Ranking for CV Cap Rate: " + viewRanking(market.getMarketName(), cvCapList));
        System.out.println("Ranking for Beta Rent Growth: " + viewRanking(market.getMarketName(), betaRGList));
        System.out.println("Ranking for Beta Vacancy: " + viewRanking(market.getMarketName(), betaVacancyList));
        System.out.println("Ranking for Beta Cap Rate: " + viewRanking(market.getMarketName(), betaCapList));
        System.out.println("Ranking for Standard Deviation Rent Growth: " + viewRanking(market.getMarketName(), stdDevRGList));
        System.out.println("Ranking for Standard Deviation Vacancy: " + viewRanking(market.getMarketName(), stdDevVacList));
        System.out.println("Ranking for Standard Deviation Cap Rate: " + viewRanking(market.getMarketName(), stdDevCapList));
    }

    // Recomputes one metric set (RG/Cap/Vacancy) for an existing market.
    private static void updateMarketProcess(Scanner scanner) {
        // Identify the market to update.
        System.out.println("Enter the name of the market you want to update:");
        if (!scanner.hasNextLine()) {
            System.out.println("No market name provided.");
            return;
        }
        String updateMarketName = scanner.nextLine().trim();
        if (updateMarketName.isEmpty()) {
            System.out.println("Market name cannot be empty.");
            return;
        }

        // Confirm the target market exists before asking for values.
        Market marketUpdated = findMarketInFile(updateMarketName);

        if (marketUpdated == null) {
            System.out.println("Market not found");
            return;
        }

        // Ask which metric family should be replaced.
        System.out.println("Enter the metric you want to update:");
        System.out.println("A. Ten Year Rent Growth: ");
        System.out.println("B. Ten year Cap Rate: ");
        System.out.println("C. Ten year Vacancy: ");
        if (!scanner.hasNextLine()) {
            System.out.println("No metric selected.");
            return;
        }
        String metric = scanner.nextLine().trim().toUpperCase();

        // Dispatch to metric-specific update flow.
        if (metric.equals("A")) {
            updateMetric(updateMarketName, "Rent Growth", scanner);
        } else if (metric.equals("B")) {
            updateMetric(updateMarketName, "Cap Rate", scanner);
        } else if (metric.equals("C")) {
            updateMetric(updateMarketName, "Vacancy", scanner);
        } else {
            System.out.println("Invalid metric choice. Please enter A, B, or C.");
        }
    }

    // Reads new market metric data and matching national data, then updates rankings and market record.
    private static void updateMetric(String marketName, String metric, Scanner scanner) {
        // Read updated market values for the selected metric.
        System.out.println("Enter the " + metric.toLowerCase() + " for past 10 years, seperated by commas:");
        if (!scanner.hasNextLine()) {
            System.out.println("No market data provided.");
            return;
        }
        ArrayList<Double> updatedData = parseDoubleList(scanner.nextLine());

        // Read national reference values for the same periods.
        System.out.println("Enter the national " + metric.toLowerCase() + " for past 10 years, seperated by commas:");
        if (!scanner.hasNextLine()) {
            System.out.println("No national data provided.");
            return;
        }
        ArrayList<Double> nationalData = parseDoubleList(scanner.nextLine());

        // Validate list quality before any file writes.
        if (updatedData == null || nationalData == null) {
            System.out.println("Invalid numeric input. Please enter comma-separated numbers only.");
            return;
        }
        if (updatedData.isEmpty() || nationalData.isEmpty()) {
            System.out.println("Both market and national lists must have at least one value.");
            return;
        }
        if (updatedData.size() != nationalData.size()) {
            System.out.println("Market and national lists must have the same number of entries.");
            return;
        }

        // Build exact line prefixes used in Rankings.txt.
        String cvPrefix = "CV " + metric + " Rankings: ";
        String betaPrefix = "Beta " + metric + " Rankings: ";
        String stdDevPrefix = "Standard Deviation " + metric + " Rankings: ";

        String lineExtractCV = null;
        String lineExtractBeta = null;
        String lineExtractStdDev = null;

        // Load the existing ranking lines for this metric.
        try {
            BufferedReader reader = new BufferedReader(new FileReader("Rankings.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(cvPrefix)) {
                    lineExtractCV = line;
                } else if (line.startsWith(betaPrefix)) {
                    lineExtractBeta = line;
                } else if (line.startsWith(stdDevPrefix)) {
                    lineExtractStdDev = line;
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("The following error occured: " + e.getMessage());
            return;
        }

        // Parse comma-delimited entries into mutable lists.
        ArrayList<String> cvList = extractRankings(cvPrefix.trim(), lineExtractCV);
        ArrayList<String> betaList = extractRankings(betaPrefix.trim(), lineExtractBeta);
        ArrayList<String> stdDevList = extractRankings(stdDevPrefix.trim(), lineExtractStdDev);

        if (cvList == null || betaList == null || stdDevList == null) {
            System.out.println("Could not parse one or more ranking lines for " + metric + ".");
            return;
        }

        // Recompute values and update ranking entries.
        ArrayList<ArrayList<String>> updates = update(marketName, metric, updatedData, nationalData,
                cvList, betaList, stdDevList);

        if (updates == null || updates.size() < 3) {
            System.out.println("Failed to update rankings.");
            return;
        }

        // Write each updated ranking list back to its source line.
        writeIntoFileUpdate(cvPrefix.trim(), updates.get(1));
        writeIntoFileUpdate(betaPrefix.trim(), updates.get(0));
        writeIntoFileUpdate(stdDevPrefix.trim(), updates.get(2));
    }

    // Compares two markets side by side.
    private static void compareMarketsProcess(Scanner scanner) {
        // Request both market names in one line.
        System.out.println("Enter two markets to compare, seperated by commas:");
        if (!scanner.hasNextLine()) {
            System.out.println("No markets provided.");
            return;
        }
        String strMarkets = scanner.nextLine();

        // Guard against malformed compare input.
        if (strMarkets == null || strMarkets.trim().isEmpty()) {
            System.out.println("Please enter two market names separated by a comma.");
            return;
        }

        String[] listCompareMarkets = strMarkets.split(",");
        if (listCompareMarkets.length < 2) {
            System.out.println("Please enter exactly two market names separated by a comma.");
            return;
        }

        String marketCompareName1 = listCompareMarkets[0].trim();
        String marketCompareName2 = listCompareMarkets[1].trim();

        if (marketCompareName1.isEmpty() || marketCompareName2.isEmpty()) {
            System.out.println("Market names cannot be empty.");
            return;
        }

        // Resolve both market objects from file storage.
        Market marketCompare1 = findMarketInFile(marketCompareName1);
        Market marketCompare2 = findMarketInFile(marketCompareName2);

        if (marketCompare1 != null && marketCompare2 != null) {
            System.out.println();
            compareMarkets(marketCompare1, marketCompare2);
        } else {
            if (marketCompare1 == null) {
                System.out.println("Mistake: Market 1 is null ");
            }
            if (marketCompare2 == null) {
                System.out.println("Mistake: Market 2 is null ");
            }
        }
    }

    // Removes a market from both ranking file and market file.
    private static void removeMarketProcess(Scanner scanner) {
        // Ask for exact market name to remove.
        System.out.println("Enter the market you wish to remove:");
        if (!scanner.hasNextLine()) {
            System.out.println("No market name provided.");
            return;
        }
        String answerRemoveMarket = scanner.nextLine();

        if (answerRemoveMarket == null || answerRemoveMarket.trim().isEmpty()) {
            System.out.println("Market name cannot be empty.");
            return;
        }

        // Load all ranking lists so we can remove the market from each one.
        ArrayList<ArrayList<String>> rankings = extractAllRankings();
        if (rankings == null) {
            System.out.println("One or more of the rankings lists are null");
            return;
        }

        // Remove any ranking entry that matches the market name.
        for (ArrayList<String> rankingList : rankings) {
            Iterator<String> iterator = rankingList.iterator();
            while (iterator.hasNext()) {
                String entry = iterator.next();
                String[] parts = entry.split(" - ");
                if (parts.length < 1) {
                    continue;
                }
                String marketName = parts[0].trim();
                if (marketName.equals(answerRemoveMarket.trim())) {
                    iterator.remove();
                }
            }
        }

        // Persist each list back to its corresponding ranking line.
        writeIntoFileUpdate("CV Vacancy Rankings:", rankings.get(7));
        writeIntoFileUpdate("CV Rent Growth Rankings:", rankings.get(5));
        writeIntoFileUpdate("CV Cap Rate Rankings:", rankings.get(3));
        writeIntoFileUpdate("Beta Vacancy Rankings:", rankings.get(8));
        writeIntoFileUpdate("Beta Rent Growth Rankings:", rankings.get(6));
        writeIntoFileUpdate("Beta Cap Rate Rankings:", rankings.get(4));
        writeIntoFileUpdate("Standard Deviation Cap Rate Rankings:", rankings.get(0));
        writeIntoFileUpdate("Standard Deviation Rent Growth Rankings:", rankings.get(1));
        writeIntoFileUpdate("Standard Deviation Vacancy Rankings:", rankings.get(2));

        // Remove the market detail row from Markets.txt.
        removeMarketFromFile(answerRemoveMarket);
    }

    // Prints a table-style view of rankings.
    private static void extractRankingsProcess() {
        // Gather all ranking lists required for tabular output.
        ArrayList<ArrayList<String>> rankings = extractAllRankings();
        if (rankings == null) {
            System.out.println("One or more of the rankings lists are null");
            return;
        }

        ArrayList<String> cvCapList = rankings.get(3);
        ArrayList<String> cvRGList = rankings.get(5);
        ArrayList<String> cvVacancyList = rankings.get(7);
        ArrayList<String> betaCapList = rankings.get(4);
        ArrayList<String> betaRGList = rankings.get(6);
        ArrayList<String> betaVacancyList = rankings.get(8);
        ArrayList<String> stdDevCapList = rankings.get(0);
        ArrayList<String> stdDevRGList = rankings.get(1);
        ArrayList<String> stdDevVacList = rankings.get(2);

        // Compute a safe row count in case list sizes drift.
        int rowCount = Math.min(
                Math.min(Math.min(cvCapList.size(), cvRGList.size()), Math.min(cvVacancyList.size(), betaCapList.size())),
                Math.min(Math.min(betaRGList.size(), betaVacancyList.size()), Math.min(stdDevCapList.size(),
                        Math.min(stdDevRGList.size(), stdDevVacList.size()))));

        if (rowCount == 0) {
            System.out.println("No ranking data available.");
            return;
        }

        // Convert lists to arrays for indexed table access.
        String[] cvCapArray = cvCapList.toArray(new String[0]);
        String[] cvRGArray = cvRGList.toArray(new String[0]);
        String[] cvVacancyArray = cvVacancyList.toArray(new String[0]);
        String[] betaCapArray = betaCapList.toArray(new String[0]);
        String[] betaRGArray = betaRGList.toArray(new String[0]);
        String[] betaVacancyArray = betaVacancyList.toArray(new String[0]);
        String[] stdDevCapArray = stdDevCapList.toArray(new String[0]);
        String[] stdDevRGArray = stdDevRGList.toArray(new String[0]);
        String[] stdDevVacArray = stdDevVacList.toArray(new String[0]);

        // Print the table header once.
        System.out.printf("%-33s %-33s %-33s %-33s %-33s %-33s%n",
                "SD Cap Rate", "SD Rent Growth", "SD Vacancy",
                "Beta Cap Rate", "Beta Rent Growth", "Beta Vacancy");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        // Print one table row per rank position.
        for (int i = 0; i < rowCount; i++) {
            String[] betaCapParts = betaCapArray[i].split(" - ");
            String[] betaRGParts = betaRGArray[i].split(" - ");
            String[] betaVacParts = betaVacancyArray[i].split(" - ");

            if (betaCapParts.length < 2 || betaRGParts.length < 2 || betaVacParts.length < 2) {
                continue;
            }

            String marketNameBetaCap = betaCapParts[0].trim();
            String betaCap = betaCapParts[1].trim();
            String marketNameBetaRG = betaRGParts[0].trim();
            String betaRG = betaRGParts[1].trim();
            String marketNameBetaVac = betaVacParts[0].trim();
            String betaVac = betaVacParts[1].trim();

            String marketNameStdDevCap = stdDevCapArray[i].split(" - ")[0].trim();
            String marketNameStdDevRG = stdDevRGArray[i].split(" - ")[0].trim();
            String marketNameStdDevVac = stdDevVacArray[i].split(" - ")[0].trim();

            System.out.printf("%-33s %-33s %-33s %-33s %-33s %-33s%n",
                    (i + 1) + ". " + marketNameStdDevCap,
                    (i + 1) + ". " + marketNameStdDevRG,
                    (i + 1) + ". " + marketNameStdDevVac,
                    (i + 1) + ". " + marketNameBetaCap + "(" + betaCap + ")",
                    (i + 1) + ". " + marketNameBetaRG + "(" + betaRG + ")",
                    (i + 1) + ". " + marketNameBetaVac + "(" + betaVac + ")");
        }
    }

    // Parses comma-separated numeric input into a list.
    private static ArrayList<Double> parseDoubleList(String input) {
        // Null input usually means no user data was provided.
        if (input == null) {
            return null;
        }

        // Empty input is treated as an empty series.
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<Double>();
        }

        // Parse each comma-delimited token as a double.
        String[] parts = trimmed.split(",");
        ArrayList<Double> result = new ArrayList<Double>();

        for (String part : parts) {
            String token = part.trim();
            // Reject blank tokens like "1,,3" to avoid silent bad data.
            if (token.isEmpty()) {
                return null;
            }
            try {
                result.add(Double.parseDouble(token));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    // Pulls ranking entries from one full file line.
    private static void parseRankingLine(String line, String prefix, ArrayList<String> list) {
        // Bail out if caller provided invalid parsing args.
        if (line == null || prefix == null || list == null) {
            return;
        }

        // Strip the heading and split raw ranking items.
        String data = line.replaceFirst(Pattern.quote(prefix), "");
        if (!data.trim().isEmpty()) {
            String[] rankings = data.split(",");
            for (String s : rankings) {
                // Keep only non-empty entries.
                if (!s.trim().isEmpty()) {
                    list.add(s.trim());
                }
            }
        }
    }

    // Reads Markets.txt and returns the matching market object.
    private static Market findMarketInFile(String marketName) {
        // Empty names cannot be looked up.
        if (marketName == null || marketName.trim().isEmpty()) {
            return null;
        }

        try {
            // Stream through the file and return at first match.
            BufferedReader reader = new BufferedReader(new FileReader("Markets.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip malformed lines with no market prefix.
                if (!line.contains(":")) {
                    continue;
                }

                // Parse market name and full detail sections.
                String[] nameParts = line.split(":", 2);
                String marketNameFile = nameParts[0];
                String[] marketDetails = line.split("\\|");
                Market market = extractMarket(marketNameFile, marketName, marketDetails);
                if (market != null) {
                    reader.close();
                    return market;
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("The following error occured: " + e.getMessage());
        }
        return null;
    }

    // Loads all ranking categories from Rankings.txt in a fixed index order.
    private static ArrayList<ArrayList<String>> extractAllRankings() {
        // Hold each raw file line before turning it into list form.
        String lineExtractCVCap = null;
        String lineExtractBetaCap = null;
        String lineExtractCVRG = null;
        String lineExtractBetaRG = null;
        String lineExtractCVVac = null;
        String lineExtractBetaVac = null;
        String lineExtractStdDevCap = null;
        String lineExtractStdDevRG = null;
        String lineExtractStdDevVac = null;

        // Read each known ranking line by prefix.
        try {
            BufferedReader reader = new BufferedReader(new FileReader("Rankings.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("CV Cap Rate Rankings: ")) {
                    lineExtractCVCap = line;
                } else if (line.startsWith("Beta Cap Rate Rankings: ")) {
                    lineExtractBetaCap = line;
                } else if (line.startsWith("CV Rent Growth Rankings: ")) {
                    lineExtractCVRG = line;
                } else if (line.startsWith("Beta Rent Growth Rankings: ")) {
                    lineExtractBetaRG = line;
                } else if (line.startsWith("CV Vacancy Rankings: ")) {
                    lineExtractCVVac = line;
                } else if (line.startsWith("Beta Vacancy Rankings: ")) {
                    lineExtractBetaVac = line;
                } else if (line.startsWith("Standard Deviation Cap Rate Rankings: ")) {
                    lineExtractStdDevCap = line;
                } else if (line.startsWith("Standard Deviation Rent Growth Rankings: ")) {
                    lineExtractStdDevRG = line;
                } else if (line.startsWith("Standard Deviation Vacancy Rankings: ")) {
                    lineExtractStdDevVac = line;
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("The following error occured: " + e.getMessage());
            return null;
        }

        // Require all categories, otherwise rankings are incomplete.
        if (lineExtractBetaCap == null || lineExtractBetaRG == null || lineExtractBetaVac == null
                || lineExtractCVCap == null || lineExtractCVRG == null || lineExtractCVVac == null
                || lineExtractStdDevCap == null || lineExtractStdDevRG == null || lineExtractStdDevVac == null) {
            return null;
        }

        // Parse all ranking lines into mutable lists.
        ArrayList<String> stdDevCapList = extractRankings("Standard Deviation Cap Rate Rankings:", lineExtractStdDevCap);
        ArrayList<String> stdDevRGList = extractRankings("Standard Deviation Rent Growth Rankings:", lineExtractStdDevRG);
        ArrayList<String> stdDevVacList = extractRankings("Standard Deviation Vacancy Rankings:", lineExtractStdDevVac);
        ArrayList<String> cvCapList = extractRankings("CV Cap Rate Rankings:", lineExtractCVCap);
        ArrayList<String> betaCapList = extractRankings("Beta Cap Rate Rankings:", lineExtractBetaCap);
        ArrayList<String> cvRGList = extractRankings("CV Rent Growth Rankings:", lineExtractCVRG);
        ArrayList<String> betaRGList = extractRankings("Beta Rent Growth Rankings:", lineExtractBetaRG);
        ArrayList<String> cvVacancyList = extractRankings("CV Vacancy Rankings:", lineExtractCVVac);
        ArrayList<String> betaVacancyList = extractRankings("Beta Vacancy Rankings:", lineExtractBetaVac);

        if (stdDevCapList == null || stdDevRGList == null || stdDevVacList == null
                || cvCapList == null || betaCapList == null || cvRGList == null || betaRGList == null
                || cvVacancyList == null || betaVacancyList == null) {
            return null;
        }

        // Return lists in the same index contract used by callers.
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        result.add(stdDevCapList);
        result.add(stdDevRGList);
        result.add(stdDevVacList);
        result.add(cvCapList);
        result.add(betaCapList);
        result.add(cvRGList);
        result.add(betaRGList);
        result.add(cvVacancyList);
        result.add(betaVacancyList);
        return result;
    }

    // Rewrites all ranking sections in one pass.
    private static void writeRankingsToFile(ArrayList<String> cvVacancyList, ArrayList<String> cvRGList,
                                            ArrayList<String> cvCapList, ArrayList<String> betaVacancyList,
                                            ArrayList<String> betaRGList, ArrayList<String> betaCapList,
                                            ArrayList<String> stdDevVacList, ArrayList<String> stdDevRGList,
                                            ArrayList<String> stdDevCapList) {
        try {
            // Full rewrite keeps line order stable and simple.
            FileWriter fw = new FileWriter("Rankings.txt");
            PrintWriter pw = new PrintWriter(fw);

            // Emit all categories in canonical order.
            writeRankingLine(pw, "CV Vacancy Rankings: ", cvVacancyList);
            writeRankingLine(pw, "CV Rent Growth Rankings: ", cvRGList);
            writeRankingLine(pw, "CV Cap Rate Rankings: ", cvCapList);
            writeRankingLine(pw, "Beta Vacancy Rankings: ", betaVacancyList);
            writeRankingLine(pw, "Beta Rent Growth Rankings: ", betaRGList);
            writeRankingLine(pw, "Beta Cap Rate Rankings: ", betaCapList);
            writeRankingLine(pw, "Standard Deviation Cap Rate Rankings: ", stdDevCapList);
            writeRankingLine(pw, "Standard Deviation Rent Growth Rankings: ", stdDevRGList);
            writeRankingLine(pw, "Standard Deviation Vacancy Rankings: ", stdDevVacList);

            pw.close();
        } catch (IOException e) {
            System.out.println("An error occured: " + e.getMessage());
        }
    }

    // Emits one ranking line in "name - value" comma-separated format.
    private static void writeRankingLine(PrintWriter pw, String prefix, ArrayList<String> list) {
        // Defensive null checks for shared helper usage.
        if (pw == null || prefix == null || list == null) {
            return;
        }

        // Write prefix and then comma-join entries manually.
        pw.print(prefix);
        for (int i = 0; i < list.size(); i++) {
            pw.print(list.get(i));
            if (i < list.size() - 1) {
                pw.print(",");
            }
        }
        pw.println();
    }

    // Appends one market snapshot to Markets.txt.
    private static void writeMarketToFile(Market market) {
        // Nothing to write if market object failed to build.
        if (market == null) {
            System.out.println("No market to write.");
            return;
        }

        try {
            // Append mode preserves existing market rows.
            FileWriter marketFw = new FileWriter("Markets.txt", true);
            PrintWriter marketWriter = new PrintWriter(marketFw);

            // Persist all metrics and ten-year arrays in one line format.
            marketWriter.print(market.getMarketName() + ": ");
            marketWriter.print("Standard Deviation of: " + "Vacancy: " + market.getStdDevVac() + ", "
                    + "Rent Growth: " + market.getStdDevRG() + ", " + "Cap Rate: " + market.getStdDevCap() + " | ");
            marketWriter.print("Coefficient of variation of: " + "Vacancy: " + market.getCVVacancy() + ", "
                    + "Rent Growth: " + market.getCVRG() + ", " + "Cap Rate: " + market.getCVCap() + " | ");
            marketWriter.print("Beta compared to the national index of: " + "Vacancy: " + market.getBetaVac() + ", "
                    + "Rent Growth: " + market.getBetaRG() + ", " + "Cap Rate: " + market.getBetaCap() + " | ");
            marketWriter.print("Ten Year Vacancy: " + market.getTenYrVac().toString() + " | ");
            marketWriter.print("Ten Year Rent Growth: " + market.getTenYrRG().toString() + " | ");
            marketWriter.print("Ten Year Cap Rate: " + market.getTenYrCap().toString() + " | ");
            marketWriter.println();

            marketWriter.close();
        } catch (IOException e) {
            System.out.println("The following probelm occured: " + e.getMessage());
        }
    }

    // Deletes a market line by market name.
    private static void removeMarketFromFile(String marketName) {
        // Do not attempt removals with empty keys.
        if (marketName == null || marketName.trim().isEmpty()) {
            System.out.println("Cannot remove empty market name.");
            return;
        }

        try {
            // Read, filter, and rewrite the entire market file.
            ArrayList<String> lines = readAllLines("Markets.txt");
            lines.removeIf(line -> line.split(":")[0].trim().equals(marketName.trim()));
            writeAllLines("Markets.txt", lines);
        } catch (IOException e) {
            System.out.println("The following error occured: " + e.getMessage());
        }
    }

    // Finds ranking position for a market and returns ordinal label.
    public static String viewRanking(String marketName, ArrayList<String> rankingList) {
        // Return a stable fallback when no ranking context exists.
        if (marketName == null || rankingList == null || rankingList.isEmpty()) {
            return "Not found";
        }

        // Walk list in rank order and find first matching market.
        for (int i = 0; i < rankingList.size(); i++) {
            String[] parts = rankingList.get(i).split(" - ");
            if (parts.length < 1) {
                continue;
            }

            if (parts[0].equals(marketName)) {
                int rank = i + 1;
                // Handle 11th/12th/13th suffixes correctly.
                if (rank % 100 >= 11 && rank % 100 <= 13) {
                    return rank + "th";
                }
                if (rank % 10 == 1)
                    return rank + "st";
                else if (rank % 10 == 2)
                    return rank + "nd";
                else if (rank % 10 == 3)
                    return rank + "rd";
                else
                    return rank + "th";
            }
        }
        return "Not found";
    }

    // Basic average helper.
    public static double avg(ArrayList<Double> list) {
        // Empty collections map to 0 to avoid crashes upstream.
        if (list == null || list.isEmpty()) {
            return 0;
        }

        // Sum all values and divide by list length.
        double sum = 0;
        for (double num : list) {
            sum += num;
        }
        return sum / list.size();
    }

    // Uses Apache Commons Math for standard deviation.
    public static double stdDev(ArrayList<Double> tenYr) {
        // Default to 0 if metric history is unavailable.
        if (tenYr == null || tenYr.isEmpty()) {
            return 0;
        }

        // Convert to primitive array for Commons Math API.
        StandardDeviation sd = new StandardDeviation(false);
        double[] tenYrArray = new double[tenYr.size()];
        for (int i = 0; i < tenYr.size(); i++) {
            tenYrArray[i] = tenYr.get(i);
        }
        // Keep output rounded to two decimals for display consistency.
        double std = sd.evaluate(tenYrArray);
        std = Math.round(std * 100.0) / 100.0;
        return std;
    }

    // Coefficient of variation, guarded for zero mean.
    public static double CV(double stdDev, double avg) {
        // Avoid dividing by zero when the mean is 0.
        if (avg == 0) {
            return 0;
        }

        // Convert to percentage-like scale and round.
        double CV = stdDev / avg;
        CV = Math.round(CV * 10000) / 100.0;
        return CV;
    }

    public static double variance(ArrayList<Double> tenYrMarket) {
        // Keep callers safe when no sample exists.
        if (tenYrMarket == null || tenYrMarket.isEmpty()) {
            return 0;
        }

        // Variance is standard deviation squared.
        double standardDev = stdDev(tenYrMarket);
        return standardDev * standardDev;
    }

    // Covariance of national vs market series.
    public static double covariance(ArrayList<Double> tenYrNational, ArrayList<Double> tenYrMarket) {
        // Missing data means covariance cannot be computed.
        if (tenYrNational == null || tenYrMarket == null || tenYrNational.isEmpty() || tenYrMarket.isEmpty()) {
            return 0;
        }

        // Series must align in length by year index.
        if (tenYrNational.size() != tenYrMarket.size()) {
            return 0;
        }

        // Compute covariance using population-style divisor N.
        double sum = 0;
        double tenYrNationalAvg = avg(tenYrNational);
        double tenYrAvg = avg(tenYrMarket);
        for (int n = 0; n < tenYrMarket.size(); n++) {
            sum += (tenYrNational.get(n) - tenYrNationalAvg) * (tenYrMarket.get(n) - tenYrAvg);
        }
        return sum / tenYrMarket.size();
    }

    // Beta with divide-by-zero protection.
    public static double beta(ArrayList<Double> tenYrNational, ArrayList<Double> tenYrMarket) {
        // Guard against flat national series (variance 0).
        double varianceNational = variance(tenYrNational);
        if (varianceNational == 0) {
            return 0;
        }

        // Standard beta formula: covariance / variance.
        double beta = covariance(tenYrNational, tenYrMarket) / varianceNational;
        beta = Math.round(beta * 100) / 100.0;
        return beta;
    }

    // Sort helper for "Market - value" strings.
    public static int compareMarketValues(String s1, String s2) {
        // Keep null-safe ordering behavior explicit.
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return 1;
        }
        if (s2 == null) {
            return -1;
        }

        // Parse numeric tail and compare ascending.
        try {
            String[] p1 = s1.split(" - ");
            String[] p2 = s2.split(" - ");
            if (p1.length < 2 || p2.length < 2) {
                return 0;
            }
            double val1 = Double.parseDouble(p1[1].trim());
            double val2 = Double.parseDouble(p2[1].trim());
            return Double.compare(val1, val2);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Central update path for one metric: recompute values, update ranking entries, rewrite market line.
    public static ArrayList<ArrayList<String>> update(String marketName, String metric, ArrayList<Double> updatedTenYr,
                                                      ArrayList<Double> tenYrNational, ArrayList<String> relevantCVRankings,
                                                      ArrayList<String> relevantBetaRankings, ArrayList<String> relevantStdDevRankings) {

        // Validate all required inputs before calculations.
        if (marketName == null || marketName.trim().isEmpty() || metric == null || metric.trim().isEmpty()) {
            return null;
        }
        if (updatedTenYr == null || tenYrNational == null || relevantCVRankings == null
                || relevantBetaRankings == null || relevantStdDevRankings == null) {
            return null;
        }
        if (updatedTenYr.isEmpty() || tenYrNational.isEmpty() || updatedTenYr.size() != tenYrNational.size()) {
            return null;
        }

        // Recompute derived stats from updated series.
        double stdDevNum = stdDev(updatedTenYr);
        double avgNum = avg(updatedTenYr);
        double cvNum = CV(stdDevNum, avgNum);
        double betaNum = beta(tenYrNational, updatedTenYr);

        // Keep values as strings for ranking file format.
        String stdDevStr = Double.toString(stdDevNum);
        String cvStr = Double.toString(cvNum);
        String betaStr = Double.toString(betaNum);

        // Replace the market's value inside each relevant ranking list.
        updateRankingEntry(relevantCVRankings, marketName, cvStr);
        updateRankingEntry(relevantBetaRankings, marketName, betaStr);
        updateRankingEntry(relevantStdDevRankings, marketName, stdDevStr);

        // Resort rankings after changing one market's values.
        relevantCVRankings.sort(Volatility::compareMarketValues);
        relevantBetaRankings.sort(Volatility::compareMarketValues);
        relevantStdDevRankings.sort(Volatility::compareMarketValues);

        // Return lists in caller-expected order: beta, CV, stddev.
        ArrayList<ArrayList<String>> relevantRankings = new ArrayList<ArrayList<String>>();
        relevantRankings.add(relevantBetaRankings);
        relevantRankings.add(relevantCVRankings);
        relevantRankings.add(relevantStdDevRankings);

        // Persist the updated metric values on the market record too.
        Market market = findMarketInFile(marketName);
        if (market != null) {
            updateMarketFields(market, metric, stdDevNum, cvNum, betaNum, updatedTenYr);
            updateMarketInFile(market);
        }

        return relevantRankings;
    }

    // Applies updated metric values to the in-memory market object.
    private static void updateMarketFields(Market market, String metric, double stdDev, double cv,
                                           double beta, ArrayList<Double> tenYrData) {
        // Ignore invalid calls from upstream.
        if (market == null || metric == null || tenYrData == null) {
            return;
        }

        // Only update the metric family requested by the user.
        if (metric.equals("Rent Growth")) {
            market.setBetaRG(beta);
            market.setCVRG(cv);
            market.setStdDevRG(stdDev);
            market.setTenYrRG(tenYrData);
        } else if (metric.equals("Cap Rate")) {
            market.setBetaCap(beta);
            market.setCVCap(cv);
            market.setStdDevCap(stdDev);
            market.setTenYrCap(tenYrData);
        } else if (metric.equals("Vacancy")) {
            market.setBetaVac(beta);
            market.setCVVacancy(cv);
            market.setStdDevVac(stdDev);
            market.setTenYrVac(tenYrData);
        }
    }

    // Replaces one market's value inside a ranking list.
    private static void updateRankingEntry(ArrayList<String> rankingList, String marketName, String newValue) {
        // Skip updates when inputs are incomplete.
        if (rankingList == null || marketName == null || newValue == null) {
            return;
        }

        // Find the matching market key and replace only that value.
        for (int i = 0; i < rankingList.size(); i++) {
            String[] parts = rankingList.get(i).split(" - ");
            if (parts.length < 2) {
                continue;
            }
            if (parts[0].trim().equals(marketName)) {
                rankingList.set(i, parts[0] + " - " + newValue);
            }
        }
    }

    // Rewrites the existing market line in Markets.txt.
    private static void updateMarketInFile(Market market) {
        // Market name is required as the line identifier.
        if (market == null || market.getMarketName() == null || market.getMarketName().trim().isEmpty()) {
            System.out.println("Cannot update market with empty name.");
            return;
        }

        try {
            // Replace the matching line and keep all others untouched.
            ArrayList<String> lines = readAllLines("Markets.txt");
            String marketLine = formatMarketLine(market);

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(market.getMarketName())) {
                    lines.set(i, marketLine);
                    break;
                }
            }

            writeAllLines("Markets.txt", lines);
        } catch (IOException e) {
            System.out.println("The following problem occured: " + e.getMessage());
        }
    }

    // Shared formatter for market lines in file storage.
    private static String formatMarketLine(Market market) {
        // Build output in a predictable pipe-delimited structure.
        StringBuilder sb = new StringBuilder();
        sb.append(market.getMarketName()).append(": ");
        sb.append("Standard Deviation of: ").append("Vacancy: ").append(market.getStdDevVac()).append(", ");
        sb.append("Rent Growth: ").append(market.getStdDevRG()).append(", ");
        sb.append("Cap Rate: ").append(market.getStdDevCap()).append(" | ");
        sb.append("Coefficient of variation of: ").append("Vacancy: ").append(market.getCVVacancy()).append(", ");
        sb.append("Rent Growth: ").append(market.getCVRG()).append(", ");
        sb.append("Cap Rate: ").append(market.getCVCap()).append(" | ");
        sb.append("Beta compared to the national index of: ").append("Vacancy: ").append(market.getBetaVac()).append(", ");
        sb.append("Rent Growth: ").append(market.getBetaRG()).append(", ");
        sb.append("Cap Rate: ").append(market.getBetaCap()).append(" | ");
        sb.append("Ten Year Vacancy: ").append(market.getTenYrVac().toString()).append(" | ");
        sb.append("Ten Year Rent Growth: ").append(market.getTenYrRG().toString()).append(" | ");
        sb.append("Ten Year Cap Rate: ").append(market.getTenYrCap().toString()).append(" | ");
        return sb.toString();
    }

    // Pretty-prints two markets if both exist in the in-memory list.
    public static void compareMarkets(Market market, Market market2) {
        // Fail fast on null objects.
        if (market == null || market2 == null) {
            System.out.println("Markets not found");
            return;
        }

        // This method intentionally compares only markets loaded in memory.
        if (marketList.contains(market) && marketList.contains(market2)) {
            System.out.println("Market: " + market.getMarketName());
            System.out.println("-----------------------------");
            System.out.println("Standard Deviation of:  Vacancy: " + Math.round(market.getStdDevVac() * 100) / 100.0
                    + " Rent Growth: " + Math.round(market.getStdDevRG() * 100) / 100.0 + " Cap Rate: "
                    + Math.round(market.getStdDevCap() * 100) / 100.0);
            System.out.println("Coefficient of variation of:  Vacancy: "
                    + Math.round(market.getCVVacancy() * 100) / 100.0 + " Rent Growth: "
                    + Math.round(market.getCVRG() * 100) / 100.0 + " Cap Rate: "
                    + Math.round(market.getCVCap() * 100) / 100.0);
            System.out.println("Beta compared to national index of:  Vacancy: " + market.getBetaVac() + " Rent Growth: "
                    + market.getBetaRG() + " Cap Rate: " + market.getBetaCap());
            System.out.println();
            System.out.println("Market: " + market2.getMarketName());
            System.out.println("-----------------------------");
            System.out.println("Standard Deviation of:  Vacancy: " + Math.round(market2.getStdDevVac() * 100) / 100.0
                    + " Rent Growth: " + Math.round(market2.getStdDevRG() * 100) / 100.0 + " Cap Rate: "
                    + Math.round(market2.getStdDevCap() * 100) / 100.0);
            System.out.println("Coefficient of variation of:  Vacancy: " + market2.getCVVacancy() + " Rent Growth: "
                    + market2.getCVRG() + " Cap Rate: " + market2.getCVCap());
            System.out.println("Beta compared to national index of:  Vacancy: " + market2.getBetaVac()
                    + " Rent Growth: " + market2.getBetaRG() + " Cap Rate: " + market2.getBetaCap());
        } else {
            System.out.println("Markets not found");
        }
    }

    // Parses one market line from Markets.txt into a Market object.
    public static Market extractMarket(String marketNameFile, String marketViewName, String[] marketDetails) {
        // No parsing can happen without all three inputs.
        if (marketNameFile == null || marketViewName == null || marketDetails == null) {
            return null;
        }

        // Only parse the row that matches the requested market name.
        if (marketNameFile.trim().equals(marketViewName.trim())) {
            marketDetails[0].replaceFirst("^\\S+\\s+", "");

            // Hold string forms for all numeric fields before conversion.
            String stdDevVacViewStr = null;
            String stdDevRGViewStr = null;
            String stdDevCapViewStr = null;
            String cvVacancyViewStr = null;
            String cvRGViewStr = null;
            String cvCapViewStr = null;
            String betaVacancyViewStr = null;
            String betaRGViewStr = null;
            String betaCapViewStr = null;
            String tenYrVacViewStr = null;
            String tenYrRGViewStr = null;
            String tenYrCapViewStr = null;

            // Parse each "|" section by expected position and label.
            for (int i = 0; i < marketDetails.length; i++) {
                String section = marketDetails[i].trim();

                if (i <= 2) {
                    // First three sections store scalar metrics by label.
                    Pattern labelValuePattern = Pattern
                            .compile("(Vacancy|Rent Growth|Cap Rate):\\s*([-+]?[0-9]*\\.?[0-9]+)");
                    Matcher matcher = labelValuePattern.matcher(section);

                    while (matcher.find()) {
                        String label = matcher.group(1);
                        String value = matcher.group(2);
                        if (i == 0 && label.equals("Vacancy"))
                            stdDevVacViewStr = value;
                        if (i == 0 && label.equals("Rent Growth"))
                            stdDevRGViewStr = value;
                        if (i == 0 && label.equals("Cap Rate"))
                            stdDevCapViewStr = value;
                        if (i == 1 && label.equals("Vacancy"))
                            cvVacancyViewStr = value;
                        if (i == 1 && label.equals("Rent Growth"))
                            cvRGViewStr = value;
                        if (i == 1 && label.equals("Cap Rate"))
                            cvCapViewStr = value;
                        if (i == 2 && label.equals("Vacancy"))
                            betaVacancyViewStr = value;
                        if (i == 2 && label.equals("Rent Growth"))
                            betaRGViewStr = value;
                        if (i == 2 && label.equals("Cap Rate"))
                            betaCapViewStr = value;
                    }
                } else {
                    // Remaining sections store ten-year arrays.
                    Pattern arrayPattern = Pattern.compile("Ten Year \\w+.*?:\\s*(\\[.*?\\])");
                    Matcher arrayMatcher = arrayPattern.matcher(section);

                    if (arrayMatcher.find()) {
                        String arrayValue = arrayMatcher.group(1);
                        if (i == 3 && section.contains("Ten Year Vacancy"))
                            tenYrVacViewStr = arrayValue;
                        if (i == 4 && section.contains("Ten Year Rent Growth"))
                            tenYrRGViewStr = arrayValue;
                        if (i == 5 && section.contains("Ten Year Cap Rate"))
                            tenYrCapViewStr = arrayValue;
                    }
                }
            }

            // Bail out early if required fields were not parsed from file.
            if (stdDevVacViewStr == null || stdDevRGViewStr == null || stdDevCapViewStr == null
                    || cvVacancyViewStr == null || cvRGViewStr == null || cvCapViewStr == null
                    || betaVacancyViewStr == null || betaRGViewStr == null || betaCapViewStr == null
                    || tenYrVacViewStr == null || tenYrRGViewStr == null || tenYrCapViewStr == null) {
                return null;
            }

            // Parse scalar strings to doubles in one guarded block.
            double stdDevVacView;
            double stdDevRGView;
            double stdDevCapView;
            double cvVacancyView;
            double cvRGView;
            double cvCapView;
            double betaVacancyView;
            double betaRGView;
            double betaCapView;

            try {
                stdDevVacView = Double.parseDouble(stdDevVacViewStr);
                stdDevRGView = Double.parseDouble(stdDevRGViewStr);
                stdDevCapView = Double.parseDouble(stdDevCapViewStr);
                cvVacancyView = Double.parseDouble(cvVacancyViewStr);
                cvRGView = Double.parseDouble(cvRGViewStr);
                cvCapView = Double.parseDouble(cvCapViewStr);
                betaVacancyView = Double.parseDouble(betaVacancyViewStr);
                betaRGView = Double.parseDouble(betaRGViewStr);
                betaCapView = Double.parseDouble(betaCapViewStr);
            } catch (NumberFormatException e) {
                return null;
            }

            // Parse Vacancy array payload.
            tenYrVacViewStr = tenYrVacViewStr.replaceAll("[\\[\\]\\s]", "");
            String[] strArrayVac = tenYrVacViewStr.split(",");
            ArrayList<Double> tenYrVacArrayView = new ArrayList<Double>();
            for (String s : strArrayVac) {
                if (s.trim().isEmpty()) {
                    continue;
                }
                try {
                    tenYrVacArrayView.add(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            // Parse Rent Growth array payload.
            tenYrRGViewStr = tenYrRGViewStr.replaceAll("[\\[\\]\\s]", "");
            String[] strArrayRG = tenYrRGViewStr.split(",");
            ArrayList<Double> tenYrRGArrayView = new ArrayList<Double>();
            for (String s : strArrayRG) {
                if (s.trim().isEmpty()) {
                    continue;
                }
                try {
                    tenYrRGArrayView.add(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            // Parse Cap Rate array payload.
            tenYrCapViewStr = tenYrCapViewStr.replaceAll("[\\[\\]\\s]", "");
            String[] strArrayCap = tenYrCapViewStr.split(",");
            ArrayList<Double> tenYrCapArrayView = new ArrayList<Double>();
            for (String s : strArrayCap) {
                if (s.trim().isEmpty()) {
                    continue;
                }
                try {
                    tenYrCapArrayView.add(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            // Build and cache the parsed Market object.
            Market marketView = new Market(marketNameFile, stdDevVacView, stdDevRGView, stdDevCapView, cvVacancyView,
                    cvRGView, cvCapView, betaVacancyView, betaRGView, betaCapView, tenYrRGArrayView, tenYrCapArrayView,
                    tenYrVacArrayView);
            marketList.add(marketView);
            return marketView;
        }
        return null;
    }

    // Prefix dictionary for ranking categories used in Rankings.txt.
    private static final Map<String, String> RANKING_PREFIXES = new HashMap<String, String>() {
        {
            // Keep keys aligned with caller tokens and file headings.
            put("CV Vacancy Rankings:", "CV Vacancy Rankings: ");
            put("CV Rent Growth Rankings:", "CV Rent Growth Rankings: ");
            put("CV Cap Rate Rankings:", "CV Cap Rate Rankings: ");
            put("Beta Vacancy Rankings:", "Beta Vacancy Rankings: ");
            put("Beta Rent Growth Rankings:", "Beta Rent Growth Rankings: ");
            put("Beta Cap Rate Rankings:", "Beta Cap Rate Rankings: ");
            put("Standard Deviation Cap Rate Rankings:", "Standard Deviation Cap Rate Rankings: ");
            put("Standard Deviation Rent Growth Rankings:", "Standard Deviation Rent Growth Rankings: ");
            put("Standard Deviation Vacancy Rankings:", "Standard Deviation Vacancy Rankings: ");
        }
    };

    // Extracts one ranking list based on the known line prefix.
    public static ArrayList<String> extractRankings(String relevantRankings, String lineFromReader) {
        // Missing file line means this category is currently empty.
        if (lineFromReader == null) {
            return new ArrayList<String>();
        }

        // Resolve canonical prefix and parse the line.
        String prefix = RANKING_PREFIXES.get(relevantRankings);
        if (prefix != null) {
            return parseRankingList(lineFromReader, prefix);
        }
        return new ArrayList<String>();
    }

    // Converts one ranking line into list elements.
    private static ArrayList<String> parseRankingList(String line, String prefix) {
        // Always return a non-null list for simpler callers.
        ArrayList<String> list = new ArrayList<String>();
        if (line == null || prefix == null) {
            return list;
        }

        // Remove heading and split on commas.
        String data = line.replaceFirst(Pattern.quote(prefix), "");
        if (!data.trim().isEmpty()) {
            String[] rankings = data.split(",");
            for (String s : rankings) {
                // Preserve only non-blank ranking tokens.
                if (!s.trim().isEmpty()) {
                    list.add(s.trim());
                }
            }
        }
        return list;
    }

    // Entry point used by metric updates to patch one line in Rankings.txt.
    public static void writeIntoFileUpdate(String relevantMetric, ArrayList<String> relevantRankings) {
        // Delegate to the shared line-update helper.
        updateFileLine("Rankings.txt", relevantMetric, relevantRankings);
    }

    // Updates one prefixed line in a file; appends if it doesn't exist yet.
    private static void updateFileLine(String filename, String linePrefix, ArrayList<String> data) {
        // Basic guardrail for corrupted arguments.
        if (filename == null || filename.trim().isEmpty() || linePrefix == null || data == null) {
            System.out.println("Invalid input passed to updateFileLine.");
            return;
        }

        try {
            // Load the whole file so we can replace one target line.
            ArrayList<String> allLines = readAllLines(filename);

            int lineIndex = -1;
            for (int i = 0; i < allLines.size(); i++) {
                if (allLines.get(i).startsWith(linePrefix)) {
                    lineIndex = i;
                    // Rebuild the line as "prefix value1,value2,...".
                    StringBuilder newLine = new StringBuilder();
                    newLine.append(linePrefix).append(" ");
                    for (int j = 0; j < data.size(); j++) {
                        newLine.append(data.get(j));
                        if (j < data.size() - 1) {
                            newLine.append(",");
                        }
                    }
                    allLines.set(i, newLine.toString());
                    break;
                }
            }

            // If the line doesn't exist, append a new category line.
            if (lineIndex == -1) {
                StringBuilder newLine = new StringBuilder();
                newLine.append(linePrefix).append(" ");
                for (int j = 0; j < data.size(); j++) {
                    newLine.append(data.get(j));
                    if (j < data.size() - 1) {
                        newLine.append(",");
                    }
                }
                allLines.add(newLine.toString());
            }

            // Persist updated line set to disk.
            writeAllLines(filename, allLines);
        } catch (IOException e) {
            System.out.println("The following error occured in writeIntoFile method: " + e.getMessage());
        }
    }

    // Minimal helper to load a text file into memory.
    private static ArrayList<String> readAllLines(String filename) throws IOException {
        // Keep return type non-null for all callers.
        ArrayList<String> lines = new ArrayList<String>();

        // Missing files are treated as empty content.
        File file = new File(filename);
        if (!file.exists()) {
            return lines;
        }

        // Read each line in order.
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }

    // Minimal helper to overwrite a file from in-memory lines.
    private static void writeAllLines(String filename, ArrayList<String> lines) throws IOException {
        // No-op on invalid inputs to avoid accidental corruption.
        if (filename == null || lines == null) {
            return;
        }

        // Rewrite file content exactly as provided in memory.
        FileWriter fw = new FileWriter(filename);
        PrintWriter pw = new PrintWriter(fw);
        for (String line : lines) {
            pw.println(line);
        }
        pw.close();
    }

    // Returns average for a specific market metric currently supported in this method.
    public static double marketAvg(String marketName, String metric) {
        // Lookup the requested market once and branch by metric label.
        Market market = findMarketInFile(marketName);
        if (market == null || metric == null) {
            return 0;
        }

        // Vacancy average path.
        if (metric.trim().equals("Vacancy")) {
            return Math.round(avg(market.getTenYrVac()) * 100) / 100.0;
        }
        // Cap rate average path.
        if (metric.trim().equals("Cap Rate")) {
            return Math.round(avg(market.getTenYrCap()) * 100) / 100.0;
        }
        // Rent growth average path.
        if (metric.trim().equals("Rent Growth")) {
            return Math.round(avg(market.getTenYrRG()) * 100) / 100.0;
        }
        return 0;
    }

    // Loads national RG/Vacancy/Cap series from National.txt.
    public static ArrayList<ArrayList<Double>> getNationalData() {
        // Keep the return payload shape stable: [RG, Vacancy, Cap].
        ArrayList<ArrayList<Double>> nationalData = new ArrayList<ArrayList<Double>>();
        BufferedReader readNationalInfo = null;
        ArrayList<Double> tenYrNationalRG = new ArrayList<Double>();
        ArrayList<Double> tenYrNationalVac = new ArrayList<Double>();
        ArrayList<Double> tenYrNationalCap = new ArrayList<Double>();
        try {
            // National file is expected to have one metric per line.
            readNationalInfo = new BufferedReader(new FileReader("National.txt"));
            String strNationalRG = readNationalInfo.readLine();
            if (strNationalRG != null) {
                // Remove the leading label from the first token.
                String[] listNationalRG = strNationalRG.split(",");
                listNationalRG[0] = listNationalRG[0].replaceFirst("Ten Year National Rent Growth: ", "");
                for (String x : listNationalRG) {
                    if (x.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        tenYrNationalRG.add(Double.parseDouble(x.trim()));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid national rent growth value: " + x);
                        return nationalData;
                    }
                }
            }
            String strNationalVac = readNationalInfo.readLine();
            if (strNationalVac != null) {
                // Remove the leading label from the first token.
                String[] listNationalVac = strNationalVac.split(",");
                listNationalVac[0] = listNationalVac[0].replaceFirst("Ten Year National Vacancy: ", "");
                for (String n : listNationalVac) {
                    if (n.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        tenYrNationalVac.add(Double.parseDouble(n.trim()));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid national vacancy value: " + n);
                        return nationalData;
                    }
                }
            }

            String strNationalCap = readNationalInfo.readLine();
            if (strNationalCap != null) {
                // Remove the leading label from the first token.
                String[] listNationalCap = strNationalCap.split(",");
                listNationalCap[0] = listNationalCap[0].replaceFirst("Ten Year National Cap Rate: ", "");
                for (String y : listNationalCap) {
                    if (y.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        tenYrNationalCap.add(Double.parseDouble(y.trim()));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid national cap rate value: " + y);
                        return nationalData;
                    }
                }
            }
            readNationalInfo.close();
        } catch (IOException e) {
            System.out.println("The following error occured: " + e.getMessage());
        }
        // Always return three lists so callers can index safely.
        nationalData.add(tenYrNationalRG);
        nationalData.add(tenYrNationalVac);
        nationalData.add(tenYrNationalCap);
        return nationalData;
    }
}
