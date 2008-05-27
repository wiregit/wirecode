/**
 * $RCSfile: CreatedJingleSessionListener.java,v $
 * $Revision: 1.1.2.1 $
 * $Date: 17/11/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.jingle.listeners;

import org.jivesoftware.smackx.jingle.JingleSession;

/**
 * Inteface used to dispatch a event when a Jingle session is created.
 * 
 * @author Thiago Camargo
 */
public interface CreatedJingleSessionListener {

    public void sessionCreated(JingleSession jingleSession);
    
}
