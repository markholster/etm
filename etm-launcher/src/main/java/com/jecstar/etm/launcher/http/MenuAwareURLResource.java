package com.jecstar.etm.launcher.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import org.xnio.IoUtils;

import io.undertow.UndertowLogger;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.util.StatusCodes;

/**
 * <code>URLResource</code> extension that injects the ETM main menu based on the roles the user is assigned to.
 * 
 * @author Mark Holster
 */
public class MenuAwareURLResource extends URLResource {

	private final URL url;

	public MenuAwareURLResource(URL url, URLConnection connection, String path) {
		super(url, connection, path);
		this.url = url;
	}
	
	@Override
	public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
		if (exchange.getSecurityContext() == null || exchange.getSecurityContext().getAuthenticatedAccount() == null) {
			super.serve(sender, exchange, completionCallback);
		} else {
			serveImpl((EtmAccount) exchange.getSecurityContext().getAuthenticatedAccount(), sender, exchange, -1, -1, false, completionCallback);
		}
	}
	
    @Override
    public void serveRange(Sender sender, HttpServerExchange exchange, long start, long end, IoCallback completionCallback) {
		if (exchange.getSecurityContext() == null || exchange.getSecurityContext().getAuthenticatedAccount() == null) {
			super.serveRange(sender, exchange, start, end, completionCallback);
		} else {
			serveImpl((EtmAccount) exchange.getSecurityContext().getAuthenticatedAccount(), sender, exchange, start, end, true, completionCallback);
		}
    }
	
	
    public void serveImpl(final EtmAccount etmAccount, final Sender sender, final HttpServerExchange exchange, final long start, final long end, final boolean range, final IoCallback completionCallback) {

        class ServerTask implements Runnable, IoCallback {

            private InputStream inputStream;
            private byte[] buffer;

            long toSkip = start;
            long remaining = end - start + 1;

            @Override
            public void run() {
                if (range && remaining == 0) {
                    //we are done, just return
                    IoUtils.safeClose(inputStream);
                    completionCallback.onComplete(exchange, sender);
                    return;
                }
                if (inputStream == null) {
                    try {
                        inputStream = url.openStream();
                    } catch (IOException e) {
                        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        return;
                    }
                    buffer = new byte[1024];//TODO: we should be pooling these
                }
                try {
                    int res = inputStream.read(buffer);
                    if (res == -1) {
                        //we are done, just return
                        IoUtils.safeClose(inputStream);
                        completionCallback.onComplete(exchange, sender);
                        return;
                    }
                    int bufferStart = 0;
                    int length = res;
                    if (range && toSkip > 0) {
                        //skip to the start of the requested range
                        //not super efficient, but what can you do
                        while (toSkip > res) {
                            toSkip -= res;
                            res = inputStream.read(buffer);
                            if (res == -1) {
                                //we are done, just return
                                IoUtils.safeClose(inputStream);
                                completionCallback.onComplete(exchange, sender);
                                return;
                            }
                        }
                        bufferStart = (int) toSkip;
                        length -= toSkip;
                        toSkip = 0;
                    }
                    if (range && length > remaining) {
                        length = (int) remaining;
                    }
                    sender.send(ByteBuffer.wrap(buffer, bufferStart, length), this);
                } catch (IOException e) {
                    onException(exchange, sender, e);
                }

            }

            @Override
            public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                } else {
                    run();
                }
            }

            @Override
            public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
                UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
                IoUtils.safeClose(inputStream);
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                }
                completionCallback.onException(exchange, sender, exception);
            }
        }

        ServerTask serveTask = new ServerTask();
        if (exchange.isInIoThread()) {
            exchange.dispatch(serveTask);
        } else {
            serveTask.run();
        }
    }


}
