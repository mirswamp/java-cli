package org.continuousassurance.swamp.session.util;

import edu.uiuc.ncsa.security.core.exceptions.NFWException;
import org.continuousassurance.swamp.session.Session;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility for working with {@link Session} objects.
 * <p>Created by Jeff Gaynor<br>
 * on 3/31/16 at  1:54 PM
 */
public class Sessions {
    /**
     * Serialize a single {@link Session} object to an {@link OutputStream}.
     *
     * @param session
     * @param outputStream
     * @throws IOException
     */
    public static void serialize(Session session, OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        singleSerialize(session, oos);
        oos.close();
    }

    /**
     * Deserialize a Session from the given {@link InputStream}
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Session deserialize(InputStream inputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        Session session = singleDeserialize(ois);
        ois.close();
        return session;
    }

    /**
     * Serialize a list of sessions. Note that these will be deserialized in exactly the order received.
     *
     * @param sessions
     * @param outputStream
     * @throws IOException
     */
    public static void serialize(List<Session> sessions, OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        for (Session session : sessions) {
            singleSerialize(session, oos);
        }
        oos.flush();
        oos.close();
        return;
    }

    /**
     * Deserialize a list of sessions. The list is cleared before writing to it if it is not empty.
     *
     * @param sessions    An empty list to hold the sessions.
     * @param inputStream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void deserialize(List<Session> sessions, InputStream inputStream) throws IOException, ClassNotFoundException {
        if (!sessions.isEmpty()) {
            sessions.clear();
        }
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        Session s = singleDeserialize(ois);
        while (s != null) {
            sessions.add(s);
            s = singleDeserialize(ois);
        }
        ois.close();
    }

    /**
     * Convenience to create a list of sessions and deserialize into it from an {@link InputStream}.
     * @param inputStream
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static List<Session> deserializeSession(InputStream inputStream) throws IOException, ClassNotFoundException {
        ArrayList<Session> sessions = new ArrayList<>();
        deserialize(sessions, inputStream);
        return sessions;
    }

    /**
     * Internal method to serialize a single session to the given {@link ObjectOutputStream}. The stream is
     * not closed at the end, merely {@link OutputStream#flush()} is called.
     *
     * @param session
     * @param oos
     * @throws IOException
     */
    protected static void singleSerialize(Session session, ObjectOutputStream oos) throws IOException {
        CookieStore cookieStore = session.getClient().getContext().getCookieStore();
        if (cookieStore instanceof BasicCookieStore) {
            BasicCookieStore bcs = (BasicCookieStore) cookieStore;
            oos.writeObject(session);
            oos.writeObject(bcs);
            oos.flush();
            return;
        }
        throw new NFWException("Error: The cookie store is not a BasicCookieStore. Cannot serialize.");
    }

    /**
     * Internal call to deserialize a single session from a stream.
     * If there is no object in the stream, a null will be returned.
     *
     * @param ois
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected static Session singleDeserialize(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        Session session = null;
        try {
            session = (Session) ois.readObject();
        } catch (EOFException eofx) {
            // no other way to do this, apparently.
            return null;
        }
        BasicCookieStore bcs = (BasicCookieStore) ois.readObject();
        session.getClient().getContext().setCookieStore(bcs);
        // setState(session);
        return session;
    }


}
