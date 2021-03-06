package in.bhargavrao.stackoverflow.natty.commands;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.User;
import in.bhargavrao.stackoverflow.natty.entities.Natty;
import in.bhargavrao.stackoverflow.natty.entities.Post;
import in.bhargavrao.stackoverflow.natty.entities.PostReport;
import in.bhargavrao.stackoverflow.natty.utils.CommandUtils;
import in.bhargavrao.stackoverflow.natty.utils.FilePathUtils;
import in.bhargavrao.stackoverflow.natty.utils.FileUtils;
import in.bhargavrao.stackoverflow.natty.utils.PostPrinter;
import in.bhargavrao.stackoverflow.natty.utils.PostUtils;
import in.bhargavrao.stackoverflow.natty.utils.PrintUtils;
import in.bhargavrao.stackoverflow.natty.utils.SentinelUtils;
import in.bhargavrao.stackoverflow.natty.validators.Validator;

/**
 * Created by bhargav.h on 28-Oct-16.
 */
public class Report implements SpecialCommand {

    private Message message;
    private Validator validator;

    public Report(Message message, Validator validator) {
        this.message = message;
        this.validator = validator;
    }

    @Override
    public boolean validate() {
        return CommandUtils.checkForCommand(message.getPlainContent(),"report");
    }

    @Override
    public void execute(Room room) {
        try {


            String word = CommandUtils.extractData(message.getPlainContent()).trim();

            if(word.contains("/"))
            {
                word = CommandUtils.getAnswerId(word);
            }

            if(FileUtils.checkIfInFile(FilePathUtils.outputReportLogFile,word)){
                room.replyTo(message.getId(), "Post already reported");
            }
            else if(FileUtils.readLineFromFileStartswith(FilePathUtils.outputCSVLogFile,"tp,"+word)!=null) {
                room.replyTo(message.getId(), "Post already registered as True Positive");
            }
            else if(FileUtils.readLineFromFileStartswith(FilePathUtils.outputCSVLogFile,"fp,"+word)!=null) {
                room.replyTo(message.getId(), "Post already registered as False Positive");
            }
            else if(FileUtils.readLineFromFileStartswith(FilePathUtils.outputCSVLogFile,"ne,"+word)!=null) {
                room.replyTo(message.getId(), "Post already registered as Needs Edit");
            }
            else if(FileUtils.readLineFromFileStartswith(FilePathUtils.outputCSVLogFile,"tn,"+word)!=null) {
                room.replyTo(message.getId(), "Post already registered as True Negative");
            }
            else {
                Natty cc = new Natty();
                Post np = cc.checkPost(Integer.parseInt(word));


                if(validator.validate(np)) {

                    PostReport report = PostUtils.getNaaValue(np);

                    String completeLog = "tn," + np.getAnswerID() + "," + np.getAnswerCreationDate() + "," + report.getNaaValue() + "," + np.getBodyMarkdown().length() + "," + np.getAnswerer().getReputation() + "," + report.getCaughtFor().stream().collect(Collectors.joining(";")) + ";";
                    FileUtils.appendToFile(FilePathUtils.outputCSVLogFile, completeLog);

                    long postId = PostUtils.addSentinel(report);
                    String description;

                    if (postId == -1) {
                        description = ("[ [Natty](" + PrintUtils.printStackAppsPost() + ") | [FMS](" + PostUtils.addFMS(report) + ") ]");
                    } else {
                        description = ("[ [Natty](" + PrintUtils.printStackAppsPost() + ") | [Sentinel](" + SentinelUtils.sentinelMainUrl + "/posts/" + postId + ") ]");
                    }
                    PostPrinter pp = new PostPrinter(np, description);
                    pp.addQuesionLink();

                    Double found = report.getNaaValue();
                    List<String> caughtFilters = report.getCaughtFor();

                    for (String filter : caughtFilters) {
                        pp.addMessage(" **" + filter + "**; ");
                    }

                    pp.addMessage(" **" + found + "**;");

                    User user = message.getUser();
                    PostUtils.addFeedback(postId, user.getId(), user.getName(), "tn");
                    //FileUtils.appendToFile(FilePathUtils.outputSentinelIdLogFile,report.getPost().getAnswerID()+","+postId);

                    room.send(pp.print());
                }
                else {
                    room.send("Post is not allowed to be reported in this room.");
                }
            }
        }
        catch (IOException e){
            e.printStackTrace();
            room.replyTo(message.getId(), "Error occurred, Try again");
        }
    }

    @Override
    public String description() {
        return "Reports the mentioned post as a true negative NAA/VLQ";
    }

    @Override
    public String name() {
        return "report";
    }
}
