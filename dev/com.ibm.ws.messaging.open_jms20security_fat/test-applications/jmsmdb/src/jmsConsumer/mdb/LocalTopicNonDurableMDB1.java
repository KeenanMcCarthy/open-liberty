/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jmsConsumer.mdb;

import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven
public class LocalTopicNonDurableMDB1 implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("Message Received in MDB1: "
                               + ((TextMessage) message).getText());

        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

}
