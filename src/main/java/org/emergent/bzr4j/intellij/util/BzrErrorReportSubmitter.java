/*
 * Copyright (c) 2010 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.emergent.bzr4j.intellij.util;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.codec.binary.Base64;
import org.emergent.bzr4j.core.BzrMessages;
import org.emergent.bzr4j.core.utils.BzrCoreUtil;
import org.emergent.bzr4j.intellij.BzrVcsMessages;
import org.emergent.bzr4j.intellij.ui.BzrSendErrorForm;
import org.jetbrains.annotations.NonNls;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Patrick Woodworth
 */
@SuppressWarnings({ "JavaDoc" })
public class BzrErrorReportSubmitter extends ErrorReportSubmitter {

  private static final Logger LOG = Logger.getInstance(BzrErrorReportSubmitter.class.getName());

  private static final Pattern ALPHA_PATTERN = Pattern.compile("[a-zA-Z]");

  private static int previousExceptionThreadId = 0;
  private static boolean wasException = false;
  @NonNls
  private static final String URL_HEADER = "http://www.intellij.net/tracker/idea/viewSCR?publicId=";

  private static final String ERROR_REPORT = BzrVcsMessages.message("error.report.title");

//  private final Timer sm_timer = new Timer("BzrErrorReportSubmitter timer", true);

  public BzrErrorReportSubmitter() {
//    LOG.debug("init()");
  }

  @Override
  public String getReportActionText() {
    return "Report to Emergent.org";
  }

  @Override
  public SubmittedReportInfo submit(IdeaLoggingEvent[] events, Component parentComponent) {
    return sendError(events[0], parentComponent);
  }

  /**
   * @noinspection ThrowablePrintStackTrace,ThrowableResultOfMethodCallIgnored
   */
  private static SubmittedReportInfo sendError(IdeaLoggingEvent event, Component parentComponent) {
    NotifierBean notifierBean = new NotifierBean();
    ErrorBean errorBean = new ErrorBean();
    errorBean.autoInit();
//    errorBean.setLastAction(IdeaLogger.ourLastActionId);

    int threadId = 0;
    SubmittedReportInfo.SubmissionStatus submissionStatus = SubmittedReportInfo.SubmissionStatus.FAILED;

    final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
    Project project = PlatformDataKeys.PROJECT.getData(dataContext);

    String description = "";
    do {
      // prepare
      try {
        ErrorReportSender sender = ErrorReportSender.getInstance();
//
        sender.prepareError(project, event.getThrowable());
//
        BzrSendErrorForm dlg = new BzrSendErrorForm();
        dlg.setErrorDescription(description);
        dlg.show();

        BzrErrorReportConfigurable reportConf = BzrErrorReportConfigurable.getInstance();
        @NonNls String senderEmail = reportConf.EMAIL_ADDRESS;
        @NonNls String smtpServer = reportConf.SMTP_SERVER;
        @NonNls String itnLogin = reportConf.AUTH_USERNAME;
        @NonNls String itnPassword = reportConf.getPlainItnPassword();
        notifierBean.setEmailAddress(senderEmail);
        notifierBean.setSmtpServer(smtpServer);
        notifierBean.setItnLogin(itnLogin);
        notifierBean.setItnPassword(itnPassword);
//
        description = dlg.getErrorDescription();
        String message = event.getMessage();
//
        @NonNls StringBuilder descBuilder = new StringBuilder();
        if (description.length() > 0) {
          descBuilder.append("User description: ").append(description).append("\n");
        }
        if (message != null) {
          descBuilder.append("Error message: ").append(message).append("\n");
        }

        Throwable t = event.getThrowable();
        if (t != null) {
//          final PluginId pluginId = IdeErrorsDialog.findPluginId(t);
//          if (pluginId != null) {
//            final IdeaPluginDescriptor ideaPluginDescriptor = ApplicationManager.getApplication().getPlugin(pluginId);
//            if (ideaPluginDescriptor != null && !ideaPluginDescriptor.isBundled()) {
//              descBuilder.append("Plugin version: ").append(ideaPluginDescriptor.getVersion()).append("\n");
//            }
//          }
        }

        if (previousExceptionThreadId != 0) {
          descBuilder.append("Previous exception is: ").append(URL_HEADER).append(previousExceptionThreadId)
              .append("\n");
        }
        if (wasException) {
          descBuilder.append("There was at least one exception before this one.\n");
        }

        errorBean.setDescription(descBuilder.toString());

        if (dlg.isShouldSend()) {
          threadId = sender.sendError(notifierBean, errorBean);
          previousExceptionThreadId = threadId;
          wasException = true;
          submissionStatus = SubmittedReportInfo.SubmissionStatus.NEW_ISSUE;

          Messages.showInfoMessage(parentComponent,
              BzrVcsMessages.message("error.report.confirmation"),
              ERROR_REPORT);
          break;
        } else {
          break;
        }

//      } catch (NoSuchEAPUserException e) {
//        if (Messages.showYesNoDialog(parentComponent, DiagnosticBundle.message("error.report.authentication.failed"),
//                                     ReportMessages.ERROR_REPORT, Messages.getErrorIcon()) != 0) {
//          break;
//        }
//      } catch (InternalEAPException e) {
//        if (Messages.showYesNoDialog(parentComponent, DiagnosticBundle.message("error.report.posting.failed", e.getMessage()),
//                                     ReportMessages.ERROR_REPORT, Messages.getErrorIcon()) != 0) {
//          break;
//        }
//      } catch (IOException e) {
//        if (!IOExceptionDialog.showErrorDialog(BzrVcsMessages.message("error.report.exception.title"),
//                                               BzrVcsMessages.message("error.report.failure.message"))) {
//          break;
//        }
//      } catch (NewBuildException e) {
//        Messages.showMessageDialog(parentComponent,
//                                   DiagnosticBundle.message("error.report.new.eap.build.message", e.getMessage()), CommonBundle.getWarningTitle(),
//                                   Messages.getWarningIcon());
//        break;
      } catch (Exception e) {
        LOG.info(e);
        if (Messages.showYesNoDialog(JOptionPane.getRootFrame(), BzrVcsMessages.message("error.report.sending.failure"),
            ERROR_REPORT, Messages.getErrorIcon()) != 0) {
          break;
        }
      }

    }
    while (true);

    return new SubmittedReportInfo(
        submissionStatus != SubmittedReportInfo.SubmissionStatus.FAILED ? URL_HEADER + threadId : null,
        String.valueOf(threadId),
        submissionStatus);
  }

  private static String stackToString(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    pw.flush();
    return sw.toString();
  }

  public static class NotifierBean {

    private String m_emailAddress;
    private String m_smtpServer;
    private String m_itnLogin;
    private String m_itnPassword;

    public String getItnLogin() {
      return m_itnLogin;
    }

    public void setItnLogin(String login) {
      m_itnLogin = login;
    }

    public String getItnPassword() {
      return m_itnPassword;
    }

    public void setItnPassword(String itnPassword) {
      m_itnPassword = itnPassword;
    }

    public String getEmailAddress() {
      return m_emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
      m_emailAddress = emailAddress;
    }

    public String getSmtpServer() {
      return m_smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
      m_smtpServer = smtpServer;
    }
  }

  public static class ErrorBean {

    private String m_description;

    public void autoInit() {
    }

    public String getDescription() {
      return m_description;
    }

    public void setDescription(String description) {
      m_description = description;
    }
  }

  public static class ErrorReportSender {

    private static final String DEFAULT_SMTP_SERVER = "smtp.easydns.com";

    private static final String SMTP_RECIPIENT = "bugreport@emergent.org";

    private static ErrorReportSender ourInstance = new ErrorReportSender();

    private StringBuffer m_body;

    private ErrorReportSender() {
    }

    public static ErrorReportSender getInstance() {
      return ourInstance;
    }

    public void prepareError(Project project, Throwable throwable) {
      StringBuffer content = new StringBuffer();
      content.append(BzrCoreUtil.dumpSystemProperties(getImportantProperties(), "stats"));
      content.append("\n");
      content.append(stackToString(throwable));
      content.append("\n");
      content.append(BzrCoreUtil.dumpSystemProperties(System.getProperties(), "system"));
      m_body = content;
    }

    public int sendError(NotifierBean notifierBean, ErrorBean errorBean) throws Exception {
      Session mailSession = getSession(notifierBean);
      mailSession.setDebug(true); // uncomment for debugging infos to stdout
      Transport transport = mailSession.getTransport();

      MimeMessage message = new MimeMessage(mailSession);
      String contentStr = errorBean.getDescription() + "\n" + m_body.toString();
      LOG.debug(contentStr);
      message.setContent(contentStr, "text/plain");
      String senderEmail = notifierBean.getEmailAddress();
      if (senderEmail == null || senderEmail.trim().length() < 1 || senderEmail.indexOf('@') < 0) {
        senderEmail = System.getProperty("user.name") + "@" + getLocalFqdn();
      }
      LOG.debug("senderEmail: " + senderEmail);
      message.setFrom(new InternetAddress(senderEmail));
      message.setSubject("BUGREPORT: Bzr4IntelliJ");
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(SMTP_RECIPIENT));
      message.saveChanges();
      transport.connect();
      transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
      transport.close();
      return 0;
    }

    private Properties getImportantProperties() {
      Properties retval = new Properties();
      retval.setProperty("bzr4j.version", BzrMessages.message("bzr4j.version"));
      retval.setProperty("idea.version", ApplicationInfo.getInstance().getBuild().asString());
      return retval;
    }

    private static String getLocalFqdn() {
      String retval = "localhost";
      try {
        InetAddress inetAddr = InetAddress.getLocalHost();
        try {
          String candidate = inetAddr.getCanonicalHostName();
          if (ALPHA_PATTERN.matcher(candidate).find())
            return candidate;
        } catch (Exception ignored) {
        }
        retval = inetAddr.getHostName();
      } catch (UnknownHostException e) {
        LOG.warn(e);
      }
      return retval;
    }

    private Session getSession(NotifierBean notifierBean) {
      Properties props = new Properties();
      props.put("mail.transport.protocol", "smtp");
//      props.put("mail.smtp.host", SMTP_HOST_NAME);
      String localFqdn = getLocalFqdn();
      String smtpServer = notifierBean.getSmtpServer();
      if (smtpServer == null || "".equals(smtpServer.trim()))
        smtpServer = DEFAULT_SMTP_SERVER;
      props.put("mail.smtp.host", smtpServer);
      LOG.debug("localFqdn: " + localFqdn);
      props.put("mail.smtp.localhost", localFqdn);
      Authenticator auth = null;
      String pword = notifierBean.getItnPassword();
      if (pword != null && pword.length() > 0) {
        LOG.debug("AUTH yes!");
        props.put("mail.smtp.auth", "true");
        auth = new SMTPAuthenticator(notifierBean.getItnLogin(), pword);
      } else {
        LOG.debug("AUTH no!");
      }
      return Session.getInstance(props, auth);
    }
  }

  private static class SMTPAuthenticator extends javax.mail.Authenticator {

    private final String m_authUsername;
    private final String m_authPassword;

    private SMTPAuthenticator(String authUsername, String authPassword) {
      m_authUsername = authUsername;
      m_authPassword = authPassword;
    }

    public PasswordAuthentication getPasswordAuthentication() {
      String username = m_authUsername;
      String password = m_authPassword;
      return new PasswordAuthentication(username, password);
    }
  }

  @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
  public static void main(String[] args) throws Exception {
    Exception e = new Exception("test exception");
    IdeaLoggingEvent ev = new IdeaLoggingEvent("a test exception occurred", e);
    BzrErrorReportSubmitter submitter = new BzrErrorReportSubmitter();
    SubmittedReportInfo reportInfo = submitter.submit(new IdeaLoggingEvent[] { ev }, null);
    System.out.printf("url: \"%s\"\nlinkText: \"%s\"\nstatus: \"%s\"\n",
        reportInfo.getURL(), reportInfo.getLinkText(), reportInfo.getStatus());
  }

}
