import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;


public class SecretPolynomial {

    public static void main(String[] args) throws Exception {
        // Read the JSON input file containing the shares
        String content = new String(Files.readAllBytes(Paths.get("input.json")));
        
        // Parse the JSON manually to extract parameters and shares
        int k = extractParameterK(content);
        List<BigInteger[]> points = extractDataPoints(content);
        
        // Perform Lagrange interpolation to find the secret
        BigDecimal secret = performLagrangeInterpolation(points, k);
        
        // Display the result
        System.out.println("Secret (constant term): " + secret.setScale(0, RoundingMode.HALF_UP));
    }
    
    /**
     * Extracts the 'k' parameter (minimum shares needed) from JSON content
     */
    private static int extractParameterK(String content) {
        Pattern kPattern = Pattern.compile("\"k\"\\s*:\\s*(\\d+)");
        Matcher kMatcher = kPattern.matcher(content);
        return kMatcher.find() ? Integer.parseInt(kMatcher.group(1)) : 0;
    }
    
    /**
     * Extracts all data points (x, y coordinates) from JSON content
     * Each point has an x-coordinate (the key) and y-coordinate (decoded from base)
     */
    private static List<BigInteger[]> extractDataPoints(String content) {
        List<BigInteger[]> points = new ArrayList<>();
        
        // Regular expression to match each numbered entry with base and value
        Pattern entryPattern = Pattern.compile(
            "\"(\\d+)\"\\s*:\\s*\\{[^}]*\"base\"\\s*:\\s*\"(\\d+)\"[^}]*\"value\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}"
        );
        Matcher entryMatcher = entryPattern.matcher(content);
        
        while (entryMatcher.find()) {
            // Extract x-coordinate (the key number)
            BigInteger x = new BigInteger(entryMatcher.group(1));
            
            // Extract base and encoded value
            int base = Integer.parseInt(entryMatcher.group(2));
            String encodedValue = entryMatcher.group(3);
            
            // Decode the y-coordinate from the specified base
            BigInteger y = new BigInteger(encodedValue, base);
            
            points.add(new BigInteger[]{x, y});
            
            System.out.println("Point " + x + ": " + encodedValue + " (base " + base + ") = " + y);
        }
        
        return points;
    }
    
    /**
     * Performs Lagrange interpolation to find the polynomial's constant term (secret)
     * at x = 0 using the first k points
     */
    private static BigDecimal performLagrangeInterpolation(List<BigInteger[]> points, int k) {
        System.out.println("\n=== Performing Lagrange Interpolation ===");
        System.out.println("Using first " + k + " points to reconstruct the secret...");
        
        // Use only the first k points for interpolation
        List<BigInteger[]> selectedPoints = points.subList(0, k);
        
        BigDecimal secret = BigDecimal.ZERO;
        
        // Lagrange interpolation formula: f(0) = Σ(yi * Li(0))
        for (int i = 0; i < k; i++) {
            BigDecimal xi = new BigDecimal(selectedPoints.get(i)[0]);
            BigDecimal yi = new BigDecimal(selectedPoints.get(i)[1]);
            
            // Calculate Lagrange basis polynomial Li(0)
            BigDecimal li = calculateLagrangeBasis(selectedPoints, i, k);
            
            // Add this term to the secret
            secret = secret.add(yi.multiply(li));
            
            System.out.println("Term " + (i + 1) + ": y" + (i + 1) + " * L" + (i + 1) + "(0) = " + 
                             yi + " * " + li.setScale(10, RoundingMode.HALF_UP));
        }
        
        return secret;
    }
    
    /**
     * Calculates the Lagrange basis polynomial Li(0) for the i-th point
     */
    private static BigDecimal calculateLagrangeBasis(List<BigInteger[]> points, int i, int k) {
        BigDecimal li = BigDecimal.ONE;
        BigDecimal xi = new BigDecimal(points.get(i)[0]);
        
        for (int j = 0; j < k; j++) {
            if (i == j) continue;
            
            BigDecimal xj = new BigDecimal(points.get(j)[0]);
            
            // Li(0) = Π((0 - xj) / (xi - xj)) for all j ≠ i
            BigDecimal numerator = xj.negate();      // (0 - xj)
            BigDecimal denominator = xi.subtract(xj); // (xi - xj)
            
            li = li.multiply(numerator.divide(denominator, 50, RoundingMode.HALF_UP));
        }
        
        return li;
    }
}
