package app.util;

import java.util.HashMap;
import java.util.Map;

public class SQLCheap {
    public static boolean isNumber(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }
    
    void updateCommand(String table,
                       HashMap<String, String> map,
                       String suffix) {
        StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
        sqlBuilder.append(table);
        sqlBuilder.append(" SET ");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            // todo
        }

    }
}
