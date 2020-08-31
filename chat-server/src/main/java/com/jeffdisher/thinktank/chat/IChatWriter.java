package com.jeffdisher.thinktank.chat;

import java.io.Closeable;
import java.util.UUID;


/**
 * Interface that describes the mechanism which allows posting to the chatroom.  The implementation is responsible for
 * sending data to the ChatStore when it arrives.
 */
public interface IChatWriter extends Closeable {
	/**
	 * Posts a message to the chat room which will be asynchronously sent to all attached listeners.
	 * 
	 * @param uuid The UUID of the user posting the message.
	 * @param message The message posted.
	 */
	void post(UUID uuid, String message);
}
