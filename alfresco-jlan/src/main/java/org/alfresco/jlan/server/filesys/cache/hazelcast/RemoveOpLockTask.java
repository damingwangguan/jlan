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

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.filesys.cache.cluster.ClusterFileState;

import com.hazelcast.core.IMap;

/**
 * Rename File State Task Class
 *
 * <p>Used to synchronize renaing a file state by executing on the remote node that owns the file state/key.
 *
 * @author gkspencer
 */
public class RemoveOpLockTask extends RemoteStateTask<Boolean> {

	// Serialization id

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public RemoveOpLockTask() {
	}

	/**
	 * Class constructor
	 *
	 * @param mapName String
	 * @param key String
	 * @param debug boolean
	 * @param timingDebug boolean
	 */
	public RemoveOpLockTask( String mapName, String key, boolean debug, boolean timingDebug) {
		super( mapName, key, true, false, debug, timingDebug);
	}

	/**
	 * Run a remote task against a file state
	 *
	 * @param stateCache IMap<String, ClusterFileState>
	 * @param fState ClusterFileState
	 * @return Boolean
	 * @exception Exception
	 */
	protected Boolean runRemoteTaskAgainstState( IMap<String, ClusterFileState> stateCache, ClusterFileState fState)
		throws Exception {

		// DEBUG

		if ( hasDebug())
			Debug.println( "RemoveOpLockTask: Remove oplock from " + fState);

		// Remove the oplock

		fState.clearOpLock();

		// Return a success status

		return Boolean.TRUE;
	}
}
