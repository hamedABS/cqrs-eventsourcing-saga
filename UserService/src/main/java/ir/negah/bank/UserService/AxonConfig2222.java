package ir.negah.bank.UserService;

import com.thoughtworks.xstream.XStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CREATED_BY abbaszadeh
 * DATE: ۲۰۲۳/۱۱/۲۸
 * TIME: ۱۱:۳۵
 */

@Configuration
public class AxonConfig2222 {

    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();

        xStream.allowTypesByWildcard(new String[] { "ir.negah.bank.**" });
        return xStream;
    }
}
