package dynamo.nodeutilities;

/**
 * Created by StefanoFiora on 22/03/2017.
 */
public class DynamoLogger {
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
            message = message.replaceFirst("\\{\\}", arg.toString());
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
        message = addPrefix(message);
        return message;
    }

    public void info(String message, Object... args){
        System.out.println(this.format(this.applyArgsToString(message, args)));
    }

//    public void warning(String message, String... args){
//        System.out.println(this.format(this.applyArgsToString(message, args)));
//    }

    public void debug(String message, Object... args){
        if (this.level == LOG_LEVEL.DEBUG){
            System.out.println(this.format(this.applyArgsToString(message, args)));
        }
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
