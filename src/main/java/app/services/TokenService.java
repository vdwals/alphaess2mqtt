package app.services;

import app.models.AlphaEssLoadJob;
import app.models.AlphaEssSetting;
import app.models.AlphaEssToken;
import app.models.api.ResponseDto;
import app.models.api.TokenDto;
import app.services.injections.ITokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.javalite.common.JsonHelper;
import org.javalite.http.Http;
import org.javalite.http.Post;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static app.util.Tokens.APPLICATION_JSON;

public class TokenService implements ITokenService {
    @Inject private final ObjectMapper objectMapper;
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Inject
    public TokenService(ObjectMapper objectMapper) {this.objectMapper = objectMapper;}
    
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
                ResponseDto<TokenDto> loginResponseDto = objectMapper.readValue(loginResponse,
                                                                                new TypeReference<ResponseDto<TokenDto>>() {});
                TokenDto tokenDto = loginResponseDto.getData();
    
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
