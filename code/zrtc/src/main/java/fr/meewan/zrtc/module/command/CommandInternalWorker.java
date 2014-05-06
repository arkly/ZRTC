/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import fr.meewan.zrtc.utils.NetworkMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * worker effectuant els taches venant du réseau interne
 * @author Meewan
 */
public class CommandInternalWorker extends Thread
{
    private CommandServer commandServer;
    private Context  context;
    private boolean stop;
    
    public CommandInternalWorker(CommandServer commandServer, Context context) 
    {
        this.commandServer = commandServer;
        this.context= context;
        this.stop = false;
    }
    
    @Override
    public void run()
    {
        //initialisation du réseau
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.connect (CommandServer.INTERNAL_COM_ADRESS);
        while(! this.stop)
        {
            //écoute d'une demande
            String rawMessage = socket.recvStr (0);
            Map<String,String> message = new JSONDeserializer<HashMap>().deserialize(rawMessage);
            message = execute(message);
            if(message != null && Integer.parseInt(message.get("state")) <= Integer.parseInt(message.get("lifecyclestates")))
            {
               sendInNetwork(message);
            }
            if (message != null)
            {
                sendToClient(message);
            }
        }
    }
    
    private Map<String, String> execute(Map <String, String> message)
    {
        String command = message.get("command");
        boolean authorized = message.get("authorized") != null && "true".equals(message.get("authorized"));
        boolean correctsignature = message.get("correctsignature")!= null && "true".equals(message.get("correctsignature"));
        String commandId = message.get("commandid");
        int state = Integer.parseInt(message.get("state")) + 1;
        int argc = Integer.parseInt(message.get("argc"));
        List<String> args = new ArrayList<>();
        for(int i = 0; i < argc; i++)
        {
            args.add(i, message.get("arg" + i));
        }
        if (!authorized)
        {
            message = NetworkMessage.generateErrorMessage(2, commandId);
        }
        else if(!correctsignature)
        {
            message = NetworkMessage.generateErrorMessage(3, commandId);
        }
        else
        {
            switch (command)
            {
                case "ping":
                {
                   CommandExternalWorker external = commandServer.getWaitingCommand(commandId);
                   if (external != null)
                   {
                       commandServer.removeFromWaitingCommands(commandId);
                       commandServer.addToActiveExternalConnexions(message.get("user"), external);
                   }
                   else 
                   {
                       return null;
                   }
                }
                    break;
                default:
                    message = NetworkMessage.generateErrorMessage(0, commandId);
                    break;
                    
            }
        }
        return message;
    }
    
    private void sendInNetwork(Map<String, String> message)
    {
         ZMQ.Socket speaker = context.socket(ZMQ.REQ);
        //on se connecte au au suivant pour qu'il complete l'objet
        speaker.connect(commandServer.getComConfiguration().get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
        //on lui passe le message
        speaker.send(new JSONSerializer().serialize(message),0);
        //on ferme la connexion (on a pas besoin de sa réponse)
        speaker.close();
    }
    
    private void sendToClient(Map<String, String> message)
    {
        CommandExternalWorker external = commandServer.getWaitingCommand(message.get("commandid"));
        if (external != null)
        {
            external.setMessage(message);
            external.wakeUp();
            commandServer.removeFromWaitingCommands(message.get("commandid"));
        }
            
    }
}