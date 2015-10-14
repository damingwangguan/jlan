/*
 * Copyright (C) 2006-2011 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.jlan.server.filesys.cache.hazelcast;

import org.alfresco.jlan.server.filesys.FileAccessToken;
import org.alfresco.jlan.server.filesys.cache.cluster.ClusterFileState;
import org.alfresco.jlan.smb.OpLock;
import org.alfresco.jlan.smb.SharingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

/**
 * Release File Access Task Class
 *
 * <p>
 * Release access to a file, and return the updated file open count.
 *
 * @author gkspencer
 */
public class ReleaseFileAccessTask extends RemoteStateTask<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseFileAccessTask.class);

    // Serialization id
    private static final long serialVersionUID = 1L;

    // Access token, allocated via a grant file access call
    private FileAccessToken m_token;

    // Cluster topic used to publish file server messages to
    private String m_clusterTopic;

    /**
     * Default constructor
     */
    public ReleaseFileAccessTask() {
    }

    /**
     * Class constructor
     *
     * @param mapName
     *            String
     * @param key
     *            String
     * @param token
     *            FileAccessToken
     * @param clusterTopic
     *            String
     * @param debug
     *            boolean
     * @param timingDebug
     *            boolean
     */
    public ReleaseFileAccessTask(final String mapName, final String key, final FileAccessToken token, final String clusterTopic, final boolean debug,
            final boolean timingDebug) {
        super(mapName, key, true, false, debug, timingDebug);

        m_token = token;
    }

    /**
     * Run a remote task against a file state
     *
     * @param stateCache
     *            IMap<String, ClusterFileState>
     * @param fState
     *            ClusterFileState
     * @return Integer
     * @exception Exception
     */
    @Override
    protected Integer runRemoteTaskAgainstState(final IMap<String, ClusterFileState> stateCache, final ClusterFileState fState) throws Exception {
        if (hasDebug()) {
            LOGGER.debug("ReleaseFileAccessTask: Release token={} path {}", m_token, fState);
        }

        // Get the current file open count
        int openCount = fState.getOpenCount();

        // Release the oplock
        if (m_token instanceof HazelCastAccessToken) {
            final HazelCastAccessToken hcToken = (HazelCastAccessToken) m_token;
            // Decrement the file open count, unless the token is from an attributes only file open
            if (hcToken.isAttributesOnly() == false) {
                // Decrement the file open count
                openCount = fState.decrementOpenCount();
                if (openCount == 0) {
                    // Reset the sharing mode and clear the primary owner, no current file opens
                    fState.setSharedAccess(SharingMode.READWRITEDELETE);
                    fState.setPrimaryOwner(null);
                }
            }

            // Check if the token indicates an oplock was granted during the file open
            if (fState.hasOpLock() && hcToken.getOpLockType() != OpLock.TypeNone) {
                // Release the remote oplock
                fState.clearOpLock();

                // Inform cluster nodes that an oplock has been released
                final ITopic<ClusterMessage> clusterTopic = getHazelcastInstance().getTopic(m_clusterTopic);
                final OpLockMessage oplockMsg = new OpLockMessage(ClusterMessage.AllNodes, ClusterMessageType.OpLockBreakNotify, fState.getPath());
                clusterTopic.publish(oplockMsg);

                if (hasDebug()) {
                    LOGGER.debug("Cleared remote oplock during token release");
                }
            }

            // This is a copy of the access token, mark it as released
            hcToken.setReleased(true);
        }

        // Return the new file open count
        return new Integer(openCount);
    }
}
