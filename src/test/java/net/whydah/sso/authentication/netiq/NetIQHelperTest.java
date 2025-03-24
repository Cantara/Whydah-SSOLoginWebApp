package net.whydah.sso.authentication.netiq;

import net.whydah.sso.authentication.UserCredential;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class NetIQHelperTest {

    /**
     * Manual test.
     * Update access clients and run.
     */
    @Ignore
    @Test
    public void testCreateUserFromNetIQRedirect() {

        NetIQHelper netIQ = new NetIQHelper();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeaderNames())
                .thenReturn(netIQ.getExpectedHeaders());

        when(request.getHeader(anyString())).thenAnswer(new Answer() {
            public String answer(InvocationOnMock invocation) {
                NetIQHelper netIQ = new NetIQHelper();
                Object[] args = invocation.getArguments();
                Object mock = invocation.getMock();
                return netIQ.getExpectedHeader((String)args[0]);
            }
        });


        assertEquals("Thor Henning", netIQ.getFirstName(request));
        assertEquals("Hetland", netIQ.getLastName(request));
        assertEquals("Thor-Henning.Hetland@altran.com", netIQ.getEmail(request));

        UserCredential userCredential= new UserCredential() {
            @Override
            public String toXML() {
                return """
                        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>\s
                         \
                        <usercredential>
                            <params>
                                <username>\
                        user\
                        </username>
                            </params>\s
                        </usercredential>
                        """;
            }
        };

    }

    @Test
    public void testHTTPHeaders() {

        NetIQHelper netIQ = new NetIQHelper();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeaderNames())
                .thenReturn(netIQ.getExpectedHeaders());

        when(request.getHeader(anyString())).thenAnswer(new Answer() {
            public String answer(InvocationOnMock invocation) {
                NetIQHelper netIQ = new NetIQHelper();
                Object[] args = invocation.getArguments();
                Object mock = invocation.getMock();
                return netIQ.getExpectedHeader((String)args[0]);
            }
        });


        Enumeration headerNames = request.getHeaderNames();
        assert(headerNames.hasMoreElements());

        while(headerNames.hasMoreElements()) {
            String headerName = (String)headerNames.nextElement();
            System.out.println("HeaderName:" + headerName);
            System.out.println("Value:" + request.getHeader(headerName));
        }
    }


}
