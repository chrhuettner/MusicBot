package com.jagrosh.jmusicbot.commands.dj;


import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.DJCommand;
import com.jagrosh.jmusicbot.queue.AbstractQueue;

/**
 * Command that provides users the ability to move a track in the playlist.
 */
public class MoveTrackCmd extends DJCommand
{
    private static final String COMMAND_NAME = "movetrack";

    public MoveTrackCmd()
    {
        super(COMMAND_NAME);
        this.help = "move a track in the current queue to a different position";
        this.arguments = "<from> <to>";
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String[] parts = event.getArgs().split("\\s+", 2);
        if (parts.length < 2) {
            event.replyError("Please include two valid indexes.");
            return;
        }

        int from = parseIndex(parts[0], event);
        int to = parseIndex(parts[1], event);

        if (from == to) {
            event.replyError("Can't move a track to the same position.");
            return;
        }

        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AbstractQueue<QueuedTrack> queue = handler.getQueue();

        if (!isValidPosition(queue, from, event) || !isValidPosition(queue, to, event)) {
            return; // Error message already sent in isValidPosition
        }

        moveTrackInQueue(queue, from, to, event);
    }

    private int parseIndex(String indexStr, CommandEvent event) {
        try {
            return Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            event.replyError("Please provide two valid indexes.");
            return -1; // Indicating an invalid index
        }
    }

    private boolean isValidPosition(AbstractQueue<QueuedTrack> queue, int position, CommandEvent event) {
        if (isUnavailablePosition(queue, position)) {
            String reply = String.format("`%d` is not a valid position in the queue!", position);
            event.replyError(reply);
            return false;
        }
        return true;
    }

    private void moveTrackInQueue(AbstractQueue<QueuedTrack> queue, int from, int to, CommandEvent event) {
        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String trackTitle = track.getTrack().getInfo().title;
        String reply = String.format("Moved **%s** from position `%d` to `%d`.", trackTitle, from, to);
        event.replySuccess(reply);
    }


    private static boolean isUnavailablePosition(AbstractQueue<QueuedTrack> queue, int position)
    {
        return (position < 1 || position > queue.size());
    }

}