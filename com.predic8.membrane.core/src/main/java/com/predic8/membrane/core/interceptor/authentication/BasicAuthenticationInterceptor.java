/* Copyright 2009 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package com.predic8.membrane.core.interceptor.authentication;

import java.util.*;

import javax.xml.stream.*;

import org.apache.commons.codec.binary.Base64;

import com.predic8.membrane.core.Constants;
import com.predic8.membrane.core.config.GenericComplexElement;
import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.*;
import com.predic8.membrane.core.interceptor.*;
import com.predic8.membrane.core.util.HttpUtil;

public class BasicAuthenticationInterceptor extends AbstractInterceptor {

	private Map<String, String> users = new HashMap<String, String>();
	
	public BasicAuthenticationInterceptor() {
		name = "Basic Authenticator";		
		setFlow(Flow.REQUEST);
	}
	
	public Outcome handleRequest(Exchange exc) throws Exception {
		
		if (hasNoAuthorizationHeader(exc) || !validUser(exc)) {
			return deny(exc);
		}
		
		return Outcome.CONTINUE;
	}

	private boolean validUser(Exchange exc) throws Exception {		
		return users.containsKey(getUsername(exc)) && 
			   users.get(getUsername(exc)).equals(getPassword(exc));
	}

	private String getUsername(Exchange exc) throws Exception {
		return getAuthorizationHeaderDecoded(exc).split(":")[0];
	}
	private String getPassword(Exchange exc) throws Exception {
		return getAuthorizationHeaderDecoded(exc).split(":")[1];
	}

	private Outcome deny(Exchange exc) {
		exc.setResponse(Response.unauthorized("").
				header("Date", HttpUtil.GMT_DATE_FORMAT.format(new Date())).
				header("Server", "Membrane-Monitor " + Constants.VERSION).
				header("WWW-Authenticate", "Basic realm=\"Membrane Authentication\"").
				header("Connection", "close").build());
		return Outcome.ABORT;
	}

	private boolean hasNoAuthorizationHeader(Exchange exc) {
		return exc.getRequest().getHeader().getFirstValue("Authorization")==null;
	}
	
	private String getAuthorizationHeaderDecoded(Exchange exc) throws Exception {
		String value = exc.getRequest().getHeader().getFirstValue("Authorization");
		return new String(Base64.decodeBase64(value.substring(6).getBytes("UTF-8")));
	}

	public Map<String, String> getUsers() {
		return users;
	}

	public void setUsers(Map<String, String> users) {
		this.users = users;
	}

	@Override
	protected void writeInterceptor(XMLStreamWriter out)
			throws XMLStreamException {
		
		out.writeStartElement("basicAuthentication");
		
		for (Map.Entry<String, String> u : users.entrySet()) {
			out.writeStartElement("user");
			
			out.writeAttribute("name", u.getKey());		
			out.writeAttribute("password", u.getValue());		

			out.writeEndElement();
		}
		
		out.writeEndElement();
	}
		
	@Override
	protected void parseChildren(XMLStreamReader token, String child)
			throws Exception {
		if (token.getLocalName().equals("user")) {
			GenericComplexElement user = new GenericComplexElement();
			user.parse(token);
			users.put(user.getAttribute("name"), user.getAttribute("password"));
		} else {
			super.parseChildren(token, child);
		}
	}
}