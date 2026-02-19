package net.whydah.sso.authentication.netiq;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.whydah.sso.config.ApplicationMode;

public class NetIQAuthTest {

    private HttpClient client;


    @BeforeClass
    public static void setup() {
      System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
    }


    /*
    @Test
    public void testUserTokenTemplate(){
        System.out.println(SSOHelper.getDummyToken());
    }
    */
    /**
     * Manual test.
     */
    @Ignore
    @Test
    public void testAuthUserFromNetIQRedirect() throws Exception  {
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


        NetIQLoginController controller = new NetIQLoginController() ;
        HttpServletResponse response = mock(HttpServletResponse.class);
        Model model = mock(Model.class);
        controller.netiqAuth(request,response,model);
    }

    /**
     * Manual test.
     */
    @Ignore
    @Test
    public void testCreateUserFromNetIQRedirect() throws Exception  {

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


        NetIQLoginController controller = new NetIQLoginController() ;
        Model model = mock(Model.class);
        controller.netIQLogin(request, model);
    }

    @Test
    public void testNetIQRedirectAndParams() throws IOException {
            String contentHtml = "https://somewhere/sso/netiqauth?redirectURI=http://somewhere";
            Pattern p = Pattern.compile("<title>(.*)</title>");
            Matcher m = p.matcher(contentHtml);
    }
}