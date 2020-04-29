/*
 * Copyright (c) 2002, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.s1asdev.ejb.mdb.msgbean.client;

import javax.naming.*;
import jakarta.jms.*;

import com.sun.ejte.ccl.reporter.SimpleReporterAdapter;

public class HelloClient {

    private static SimpleReporterAdapter stat =
        new SimpleReporterAdapter("appserv-tests");

    // in milli-seconds
    private static long TIMEOUT = 90000;

    private String[] args;
    public static void main (String[] args) {
        HelloClient client = new HelloClient(args);
        stat.addDescription("ejb-mdb-msgbean");
        client.doTest();
        stat.printSummary("ejb-mdb-msgbeanID");
        int exitCode = 0;
        System.exit(exitCode);
    }

    private InitialContext context;
    private QueueConnection queueCon;
    private QueueSession queueSession;
    private QueueSender queueSender;
    private QueueReceiver queueReceiver;
    private jakarta.jms.Queue clientQueue;


    private TopicConnection topicCon;
    private TopicSession topicSession;
    private TopicPublisher topicPublisher;
    private TopicSubscriber topicSubscriber;
    private Topic clientTopic;

    public HelloClient(String[] theArgs) {
        this.args = theArgs;
        if( this.args.length == 0 ) {
            this.args = new String[] { "all", "5" };
        }
    }

    private static Boolean BFALSE = new Boolean(false);
    private static Boolean BTRUE  = new Boolean(true);

    Object[][] userSpecifiedTxTest = { {"", // destname
                                        BFALSE, // reply
                                        BFALSE,  // jdbc
                                        BFALSE } };  // rollback

    Object[][] onePhaseCommitTests = { 
        { "jms/queue_cmt",   BFALSE, BFALSE, BFALSE },
        { "jms/topic_cmt",   BFALSE, BFALSE, BFALSE },

        { "jms/queue_bmt",   BTRUE,  BFALSE, BFALSE },
        { "jms/topic_bmt",   BTRUE,  BFALSE, BFALSE },

        { "jms/queue_bmt",   BFALSE, BTRUE,  BFALSE },
        { "jms/topic_bmt",   BFALSE, BTRUE,  BFALSE },
    };

    Object[][] twoPhaseCommitTests = { 
        { "jms/queue_cmt",   BFALSE, BTRUE,  BFALSE },
        { "jms/topic_cmt",   BFALSE, BTRUE,  BFALSE },

        { "jms/queue_cmt",   BTRUE,  BFALSE, BFALSE },
        { "jms/topic_cmt",   BTRUE,  BFALSE, BFALSE },

        { "jms/queue_bmt",   BTRUE,  BTRUE,  BFALSE },
        { "jms/topic_bmt",   BTRUE,  BTRUE,  BFALSE },

        { "jms/queue_cmt",   BTRUE,  BTRUE,  BFALSE },
        { "jms/topic_cmt",   BTRUE,  BTRUE,  BFALSE },
    };
    
    Object[][] rollbackTests = { 

        { "jms/queue_cmt",   BTRUE,  BFALSE,  BTRUE },
        { "jms/topic_cmt",   BTRUE,  BFALSE,  BTRUE },
	
        { "jms/queue_cmt",   BTRUE,  BTRUE,   BTRUE },
        { "jms/topic_cmt",   BTRUE,  BTRUE,   BTRUE },
	
        { "jms/queue_cmt",   BFALSE, BFALSE,  BTRUE },
        { "jms/topic_cmt",   BFALSE, BFALSE,  BTRUE },
	
        { "jms/queue_cmt",   BTRUE,  BFALSE,  BTRUE },
        { "jms/topic_cmt",   BTRUE,  BFALSE,  BTRUE },        
        
    };


    Object[][] localTxTests = {
        { "jms/queue_cmtns", BFALSE, BFALSE, BFALSE  },
        { "jms/queue_cmtns", BFALSE, BTRUE, BFALSE  },
        { "jms/queue_cmtns", BTRUE, BFALSE, BFALSE  },
        { "jms/queue_cmtns", BTRUE, BTRUE, BFALSE  }
    };

    public void doTest() {
        int numIter = 10;
        if(args.length == 0 || (args.length > 0 && 
                                  args[0].equals("1pc" )) ) {
            System.out.println("Running 1 phase commit msgbean tests");
            numIter = (args.length >= 2) ? 
                Integer.parseInt(args[1]) : numIter;
            doTests(onePhaseCommitTests, numIter);
        } else if( args[0].equals("2pc") ) {
            System.out.println("Running 2 phase commit msgbean tests");
            numIter = Integer.parseInt(args[1]);
            doTests(twoPhaseCommitTests, numIter);
        } else if( args[0].equals("localtx") ) {
            System.out.println("Running localtx msgbean tests");
            numIter = Integer.parseInt(args[1]);
            doTests(localTxTests, numIter);
        } else if( args[0].equals("rollback") ) {
            System.out.println("Running rollback msgbean tests");
            numIter = Integer.parseInt(args[1]);
            doTests(rollbackTests, numIter);
        } else if( args[0].equals("all") ) {
            System.out.println("Running all msgbean tests");
            numIter = Integer.parseInt(args[1]);
            doTests(onePhaseCommitTests, numIter);
            doTests(twoPhaseCommitTests, numIter);
            doTests(localTxTests, numIter);
	    doTests(rollbackTests, numIter);
        } else {
            userSpecifiedTxTest[0][0] = args[0];
            numIter = Integer.parseInt(args[1]);
            System.out.println("Running " + userSpecifiedTxTest[0][0]);
            System.out.println("Num iterations = " + numIter);
            if( args.length == 3 ) {
                userSpecifiedTxTest[0][1] = 
                    new Boolean(args[2].indexOf("reply") != -1);
                userSpecifiedTxTest[0][2] = 
                    new Boolean((args[2].indexOf("jdbc") != -1));
                userSpecifiedTxTest[0][3] = 
                    new Boolean((args[2].indexOf("rollback") != -1));
            }
            doTests(userSpecifiedTxTest, numIter);
        } 

        return;
    }

    private void doTests(Object[][] tests, int numIter) {
        if (numIter > 20) {
            TIMEOUT = TIMEOUT * (int)(numIter / 10);
        }
        
        for(int i = 0; i < tests.length; i++) {
            String destName = "java:comp/env/" + (String) tests[i][0];
            boolean expectReply = ((Boolean)tests[i][1]).booleanValue();
            boolean jdbc = ((Boolean)tests[i][2]).booleanValue();
            boolean rollback = ((Boolean)tests[i][3]).booleanValue();
            String test = "msgbean " + generateTestName(destName,jdbc,
                                                        expectReply, rollback);
            try {
                setup();
                doTest(destName, numIter, expectReply, jdbc, rollback);
                stat.addStatus(test, stat.PASS);                
            } catch(Throwable t) {
                System.out.println("Caught unexpected exception " + t);
                t.printStackTrace();
                stat.addStatus(test, stat.FAIL);                
                break;
            } finally {
                cleanup();
            }
        }
        return;
    }

    public void setup() throws Exception {
        context = new InitialContext();
        
        QueueConnectionFactory queueConFactory = 
            (QueueConnectionFactory) context.lookup
            ("java:comp/env/jms/QCF");
            
        queueCon = queueConFactory.createQueueConnection();

        queueSession = queueCon.createQueueSession
            (false, Session.AUTO_ACKNOWLEDGE); 

        // Producer will be specified when actual msg is sent.
        queueSender = queueSession.createSender(null);        

        clientQueue = (jakarta.jms.Queue) 
            context.lookup("java:comp/env/jms/client_queue");

        queueReceiver = queueSession.createReceiver(clientQueue);

        TopicConnectionFactory topicConFactory = 
            (TopicConnectionFactory) context.lookup
            ("java:comp/env/jms/TCF");
                
        topicCon = topicConFactory.createTopicConnection();

        topicSession = 
            topicCon.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            
        // Producer will be specified when actual msg is published.
        topicPublisher = topicSession.createPublisher(null);

        clientTopic = (Topic) 
            context.lookup("java:comp/env/jms/client_topic");

        topicSubscriber = topicSession.createSubscriber(clientTopic);

        queueCon.start();
        topicCon.start();
    }

    public void cleanup() {
        try {
            if( queueCon != null ) {
                queueCon.close();
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        try {
            if( topicCon != null ) {
                topicCon.close();
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    public void sendMsgs(jakarta.jms.Queue queue, Message msg, int num) 
        throws JMSException {
        for(int i = 0; i < num; i++) {
            System.out.println("Sending message " + i + " to " + queue);
            queueSender.send(queue, msg);
        }
    }

    public void publishMsgs(Topic topic, Message msg, int num) 
        throws JMSException {
        for(int i = 0; i < num; i++) {
            System.out.println("Publishing message " + i + " to " + topic);
            topicPublisher.publish(topic, msg);
        }
    }

    public void recvQueueMsgs(int num) throws JMSException {
        for(int i = 0; i < num; i++) {
            System.out.println("Waiting for queue message " + (i+1) + " of " + num);
            Message recvdmessage = queueReceiver.receive(TIMEOUT);
            if( recvdmessage != null ) {
                System.out.println("Received message : " + 
                                   ((TextMessage)recvdmessage).getText());
            } else {
                System.out.println("timeout after " + TIMEOUT + " seconds");
                throw new JMSException("timeout" + TIMEOUT + " seconds");
            }
        }
    }

    public void recvTopicMsgs(int num) throws JMSException {
        for(int i = 0; i < num; i++) {
            System.out.println("Waiting for topic message " + (i+1) + " of " + num);
            Message recvdmessage = topicSubscriber.receive(TIMEOUT);
            if( recvdmessage != null ) {
                System.out.println("Received message : " + 
                                   ((TextMessage)recvdmessage).getText());
            } else {
                System.out.println("timeout after " + TIMEOUT + " seconds");
                throw new JMSException("timeout" + TIMEOUT + " seconds");
            }
        }
    }

    /** 
     * Generate testname. (Try to keep synched with target names in Makefile.)
     */
    private String generateTestName(String destName, boolean doJdbc, 
				    boolean expectReply, boolean rollbackEnabled) 
    {
        String testName = destName.toLowerCase();
        if (doJdbc) {
            testName = testName + "_jdbc";
        } else {
            testName = testName + "_nojdbc";
        }
        if (expectReply) {
            testName = testName + "_reply";
        } else {
            testName = testName + "_noreply";
        }
        if (rollbackEnabled) {
            testName = testName + "_rollback";
        }
        return testName;
    }

    public void doTest(String destName, int numIter, boolean expectReply,
                       boolean doJdbc, boolean rollbackEnabled) 
                       
        throws Exception 
    {
        String testName = 
            generateTestName(destName, doJdbc, expectReply, rollbackEnabled);
        try {
            Destination dest = (Destination) context.lookup(destName);
            
            boolean pointToPoint = dest instanceof jakarta.jms.Queue;
            
            System.out.println("Beginning test : " + testName);
            
            Timer.start();
            
            if( pointToPoint ) {
                Message message = 
                    queueSession.createTextMessage(destName);
                message.setBooleanProperty("doJdbc", doJdbc);
                message.setBooleanProperty("rollbackEnabled", rollbackEnabled);
                
                /* aid identifying what test message is associated with on JMS server side.
                 * (JMS RI Message Implementation toString() will print this property if it
                 *  is set.)
                 */
                message.setStringProperty("COM_SUN_JMS_TESTNAME", testName + "_#" + numIter);
                if( expectReply ) {
                    message.setJMSReplyTo(clientQueue);
                }
                sendMsgs((jakarta.jms.Queue) dest, message, numIter);
            } else {
                Message message = 
                    topicSession.createTextMessage(destName);
                message.setBooleanProperty("doJdbc", doJdbc);
                message.setBooleanProperty("rollbackEnabled", rollbackEnabled);
                
                /* aid identifying what test message is associated with on JMS server side.
                 * (JMS RI Message Implementation toString() will print this property if it
                 *  is set.)
                 */
                message.setStringProperty("COM_SUN_JMS_TESTNAME", testName + "_numIter");
                if( expectReply ) {
                    message.setJMSReplyTo(clientTopic);
                }
                publishMsgs((Topic) dest, message, numIter);
            }
            
            if( expectReply ) {
                if( pointToPoint ) {
                    recvQueueMsgs(numIter);
                } else {
                    recvTopicMsgs(numIter);
                }
            }
            
            long time = Timer.stop();
            
            System.out.println("End test : " + testName + ". Time = " +
                               Timer.format(time) + " -> " + (time/numIter) +
                               " msec/msg (" + numIter + " msgs)");
        } catch (Exception e) {
            System.out.println("Unexpected exception " + e + " in test " + 
                               testName);
            throw e;
        }
    }
    
    // helper class for timing the tests
    private static class Timer {
        static long startTime;
        
        public static void start() {
            startTime = System.currentTimeMillis();
        }
        
        public static long stop() {
            return System.currentTimeMillis() - startTime;
        }
        
        /**
         * Returns a string that outputs time in an
         * easily readable format.
         */
        public static String format(long timeIn) {
            double foo = ((double)timeIn)/1000;
            long time = Math.round(foo); 
            String ret = null;
            if (time < 60) {
                ret = time + " seconds";
            } else {
                ret = (time/60) + " minutes, " +
                    (time%60) + " seconds (" +
                    time + " seconds)";
            }
            return ret;
        }
    }
}

