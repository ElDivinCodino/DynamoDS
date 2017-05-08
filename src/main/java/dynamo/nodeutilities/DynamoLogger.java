package dynamo.nodeutilities;

/**
 * Created by StefanoFiora on 22/03/2017.
 */
public class DynamoLogger {
    public static final String ANSI_PREFIX = "\u001B[";

    public static final String ANSI_RESET = ANSI_PREFIX + "0m";
    public static final String ANSI_BLACK = ANSI_PREFIX + "30m";
    public static final String ANSI_RED = ANSI_PREFIX + "31m";
    public static final String ANSI_GREEN = ANSI_PREFIX + "32m";
    public static final String ANSI_YELLOW = ANSI_PREFIX + "33m";
    public static final String ANSI_BLUE = ANSI_PREFIX + "34m";
    public static final String ANSI_PURPLE = ANSI_PREFIX + "35m";
    public static final String ANSI_CYAN = ANSI_PREFIX + "36m";
    public static final String ANSI_WHITE = ANSI_PREFIX + "37m";

    public static final String ANSI_RED_BOLD = ANSI_PREFIX + "1;31m";

    public enum LOG_LEVEL { INFO, DEBUG };
    private LOG_LEVEL level;
    private String prefix;
    private String postfix;

    public DynamoLogger(String prefix, String suffix) {
        this.prefix = prefix;
        this.postfix = suffix;
    }

    public DynamoLogger(){
        this.prefix = null;
        this.postfix = null;
    }

    private String applyArgsToString(String message, Object[] args){
        for (Object arg : args) {
            if (arg == null){
                message = message.replaceFirst("\\{\\}", "null");
            } else {
                message = message.replaceFirst("\\{\\}", arg.toString());
            }

        }
        return message;
    }

    private String addPrefix(String message){
        if (this.prefix == null){
            return message;
        }else {
            return prefix + message;
        }
    }

    private String addSuffix(String message){
        if (this.prefix == null){
            return message;
        }else {
            return message + postfix;
        }
    }

    private String format(String message){
        message = addPrefix(message);
//        message = addPrefix(message);
        return message;
    }

    public void info(String message, Object... args){
        this.prefix = ANSI_CYAN + "[INFO] " + ANSI_RESET;
        System.out.println(this.format(this.applyArgsToString(message, args)));
    }

//    public void warning(String message, String... args){
//        System.out.println(this.format(this.applyArgsToString(message, args)));
//    }

    public void debug(String message, Object... args){
        this.prefix = ANSI_YELLOW + "[DEBUG] " + ANSI_RESET;
        if (this.level == LOG_LEVEL.DEBUG){
            System.out.println(this.format(this.applyArgsToString(message, args)));
        }
    }

    public void error(String message, Object... args){
        this.prefix = ANSI_RED + "[ERROR] " + ANSI_RESET;
        System.out.println(this.format(this.applyArgsToString(message, args)));
    }

    public LOG_LEVEL getLevel() {
        return level;
    }

    public void setLevel(LOG_LEVEL level) {
        this.level = level;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }
}
