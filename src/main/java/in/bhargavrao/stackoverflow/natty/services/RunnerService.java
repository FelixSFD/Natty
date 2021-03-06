package in.bhargavrao.stackoverflow.natty.services;

import fr.tunaki.stackoverflow.chat.ChatHost;
import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.event.EventType;
import in.bhargavrao.stackoverflow.natty.clients.Runner;
import in.bhargavrao.stackoverflow.natty.entities.Natty;
import in.bhargavrao.stackoverflow.natty.entities.Post;
import in.bhargavrao.stackoverflow.natty.roomdata.BotRoom;
import in.bhargavrao.stackoverflow.natty.utils.FilePathUtils;
import in.bhargavrao.stackoverflow.natty.validators.AllowAllNewAnswersValidator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bhargav.h on 28-Dec-16.
 */
public class RunnerService {

    private StackExchangeClient client;
    private List<BotRoom> rooms;
    private List<Room> chatRooms;
    private ScheduledExecutorService executorService;


    public RunnerService(StackExchangeClient client, List<BotRoom> rooms) {
        this.client = client;
        this.rooms = rooms;
        chatRooms = new ArrayList<>();
    }

    public void start(){
        for(BotRoom room:rooms){
            Room chatroom = client.joinRoom(ChatHost.STACK_OVERFLOW ,room.getRoomId());

            if(room.getRoomId()==111347){
            	//check if Natty is running on the server
            	Properties prop = new Properties();

                try{
                    prop.load(new FileInputStream(FilePathUtils.loginPropertiesFile));
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            	
                if (prop.getProperty("location").equals("server")) {
                	chatroom.send("Hiya o/ (SERVER VERSION)" );
                    FeederService feederService = new FeederService("*Feeds @Kyll*",chatroom,8);
                    feederService.start();
                } else {
                	chatroom.send("Hiya o/ (DEVELOPMENT VERSION; "+prop.getProperty("location")+")" );
                }
            }

            chatRooms.add(chatroom);
            if(room.getMention(chatroom)!=null)
                chatroom.addEventListener(EventType.USER_MENTIONED, room.getMention(chatroom));
            if(room.getReply(chatroom)!=null)
                chatroom.addEventListener(EventType.MESSAGE_REPLY, room.getReply(chatroom));
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void run() {
        Natty cc = new Natty();
        executorService.scheduleAtFixedRate(()->execute(cc), 0, 59, TimeUnit.SECONDS);
    }

    private void execute(Natty cc) {
        List<Post> posts = null;
        try {
            posts = cc.getPosts(new AllowAllNewAnswersValidator());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i=0;i<rooms.size();i++){
            BotRoom room = rooms.get(i);
            Room chatroom = chatRooms.get(i);
            new Runner().runOnce(chatroom, room.getValidator(), posts, room.getNaaValue(), room.getPostPrinter(), room.getIsLogged());
        }
    }

    public void stop(long RoomId){

    }
}
