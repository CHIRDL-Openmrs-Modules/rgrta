package org.openmrs.module.rgrta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.hibernateBeans.ATDError;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.util.IOUtil;

public class KiteMessageHandler
{

	private static final String HL7_START_OF_MESSAGE = "\u000b";
	private static final String HL7_END_OF_MESSAGE = "\u001c";

	protected final Log log = LogFactory.getLog(getClass());
	
	private String host = null;
	private int port = 0;
	private Socket socket = null;
	private OutputStream os = null;
	private InputStream is = null;
	private int timeout = 0;

	public KiteMessageHandler(String host, int port, int timeout)
	{
		this.host = host;
		this.port = port;
		this.timeout = timeout;
	}

	/*
	 * 
	 */
	public void openSocket() throws IOException
	{
		this.socket = new Socket(this.host, this.port);
		this.socket.setSoTimeout(this.timeout);

		this.os = this.socket.getOutputStream();
		this.is = this.socket.getInputStream();
	}

	public void closeSocket()
	{
		try
		{
			Socket sckt = this.socket;
			this.socket = null;
			if (sckt != null)
				sckt.close();
		} catch (Exception e)
		{
			this.log.error(e.getMessage());
			this.log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
		}
	}

	public void sendMessage(String theMessage) throws IOException
	{
		this.os.write(theMessage.getBytes());
		this.os.write(13);
		this.os.flush();
	}

	public String getMessage()
	{
		ATDService atdService = Context.getService(ATDService.class);
		ByteArrayOutputStream outputString = new ByteArrayOutputStream();

		try
		{
			IOUtil.bufferedReadWrite(this.is, outputString,1);
		
		} catch (Exception e)
		{	
			ATDError error = new ATDError("Error","Query Kite Connection"
					, "Message dropped"
					, "Partial message: "+outputString.toString(), new Date(), null);
			atdService.saveError(error);
			return null;
		}finally{
			try {
	            outputString.flush();
	            outputString.close();
            }
            catch (IOException e) {
            }
		}
		return outputString.toString();
	}

}
