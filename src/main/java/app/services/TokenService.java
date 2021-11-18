package app.services;

import app.models.AlphaEssLoadJob;
import app.models.AlphaEssSetting;
import app.models.AlphaEssToken;
import app.models.api.LoginResponseDto;
import app.models.api.TokenDto;
import app.services.injections.ITokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static app.util.Tokens.APPLICATION_JSON;

public class TokenService implements ITokenService {
    private final ObjectMapper      objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter    = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public String getToken() {
        AlphaEssToken currentToken = AlphaEssToken.findCurrentToken();
        
        if (currentToken == null) {
            AlphaEssLoadJob loginJob = AlphaEssLoadJob.getLoginJob();
            
            Map<String, String> settings = AlphaEssSetting.getSettings();
            
            Post loginPost = Http.post(loginJob.getUrl(), JsonHelper.toJsonString(settings))
                                 .header("Accept", APPLICATION_JSON)
                                 .header("Content-Type", APPLICATION_JSON);
            
            String loginResponse = loginPost.text();
            
            try {
                LoginResponseDto loginResponseDto = objectMapper.readValue(loginResponse,
                                                                           LoginResponseDto.class);
                TokenDto         tokenDto         = loginResponseDto.getData();
                
                LocalDateTime expirationTime = LocalDateTime.parse(tokenDto.getTokenCreateTime(),
                                                                   formatter)
                                                            .plusSeconds(tokenDto.getExpiresIn());
                
                // Delete all tokens as they are expired.
                AlphaEssToken.deleteAll();
                
                // Save new token.
                currentToken = AlphaEssToken.create(tokenDto.getAccessToken(),
                                                    expirationTime,
                                                    tokenDto.getRefreshTokenKey());
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return currentToken.getToken();
    }
}
