package `in`.app.connect.utils



import java.util.*
import javax.mail.*
import javax.mail.internet.*
object EmailSender {
    @Throws(Exception::class)
    fun sendEmail(recipient: String?, subject: String?, body: String?) {
        // Configure SMTP settings
        val host = "mail.connect-app.in"//host
        val username = "contact@connect-app.in"//emailId
        val password = "Connect@1"//password
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.ssl.enable"] = "true"
        props["mail.smtp.host"] = host
        props["mail.smtp.port"] = "587"

        // Create the email message
        val session = Session.getInstance(props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(username))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
        message.subject = subject

        // Set the HTML content
        val htmlBodyPart = MimeBodyPart()
        htmlBodyPart.setContent(body, "text/html; charset=utf-8")

        // Create a Multipart object and add the HTML body part to it
        val multipart = MimeMultipart()
        multipart.addBodyPart(htmlBodyPart)

        // Set the multipart object as the message's content
        message.setContent(multipart)
        // Send the email
        try {
            val thread = Thread { Transport.send(message) }
            thread.start()
        } catch (e: MessagingException) {
            println(e.message)
        } catch (e: AddressException) {
            println(e.message)
        }
    }
}
