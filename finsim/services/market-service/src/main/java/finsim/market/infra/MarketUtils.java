package finsim.market.infra;

import finsim.market.entities.Fill;
import java.util.ArrayList;
import java.util.List;

public class MarketUtils {    
    // Generate a unique fill ID
    public static String generateFillId() {
        return "fill-" + System.currentTimeMillis() + "-" + 
               Math.round(Math.random() * 1000);
    }
}