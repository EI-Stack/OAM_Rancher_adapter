package solaris.nfm.config.security.util;

import java.io.Serializable;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClock;
import solaris.nfm.config.security.domain.JwtUser;

/**
 * jwt工具类，生成令牌
 * 从客户端获取的令牌里面获取相关字段（解密）
 */
@Component
public class JwtTokenUtil implements Serializable
{

	private static final long	serialVersionUID	= 1L;

	// 申明部分的属性名
	static final String			CLAIM_KEY_USERNAME	= "username";    // 原先是 sub
	static final String			CLAIM_KEY_ROLE		= "role";
	static final String			CLAIM_KEY_AUDIENCE	= "aud";
	static final String			CLAIM_KEY_CREATED	= "iat";

	// 签名部分的属性名
	static final String			AUDIENCE_UNKNOWN	= "unknown";
	static final String			AUDIENCE_WEB		= "web";
	static final String			AUDIENCE_MOBILE		= "mobile";
	static final String			AUDIENCE_TABLET		= "tablet";

	private Clock				clock				= DefaultClock.INSTANCE;

	@Value("${solaris.security.jwt.secret}")     // 从配置文件去获取自定义口令，然后注入到 secret 中来
	private String				secret;

	// @Value("${jwt.expiration}")
	private Long				expiration			= 604800L;

	public static String getUsernameFromTokenPayload(final JsonNode tokenBodyJson)
	{
		if (tokenBodyJson == null) return null;
		return tokenBodyJson.path(CLAIM_KEY_USERNAME).asText();
	}

	public static String getRoleFromTokenPayload(final JsonNode tokenBodyJson)
	{
		if (tokenBodyJson == null) return null;
		return tokenBodyJson.path(CLAIM_KEY_ROLE).asText();
	}

	public static JsonNode getJsonFromTokenPayload(final String token)
	{
		if (StringUtils.hasText(token) == false) return null;

		final String[] tokenSplitArray = token.split("\\.");
		if (tokenSplitArray.length != 3) return null;

		final String base64EncodedBody = (token.split("\\."))[1];
		String tokenBodyJsonString;
		JsonNode tokenBodyJson;
		try
		{
			tokenBodyJsonString = new String(Base64.getDecoder().decode(base64EncodedBody), "UTF-8");
			tokenBodyJson = (new ObjectMapper()).readTree(tokenBodyJsonString);
		} catch (final Exception e)
		{
			return null;
		}

		return tokenBodyJson;
	}

	// 从得到的令牌里面获得用户名
	public String getUsernameFromToken(final String token)
	{

		return getClaimFromToken(token, Claims::getSubject);
	}

	public Date getIssuedAtDateFromToken(final String token)
	{

		return getClaimFromToken(token, Claims::getIssuedAt);
	}

	public Date getExpirationDateFromToken(final String token)
	{
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public String getAudienceFromToken(final String token)
	{
		return getClaimFromToken(token, Claims::getAudience);
	}

	public <T> T getClaimFromToken(final String token, final Function<Claims, T> claimsResolver)
	{
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	private Claims getAllClaimsFromToken(final String token)
	{
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	private Boolean isTokenExpired(final String token)
	{
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(clock.now());
	}

	private Boolean isCreatedBeforeLastPasswordReset(final Date created, final Date lastPasswordReset)
	{
		return (lastPasswordReset != null && created.before(lastPasswordReset));
	}

	/*
	 * private String generateAudience(Device device) {
	 * String audience = AUDIENCE_UNKNOWN;
	 * if (device.isNormal()) {
	 * audience = AUDIENCE_WEB;
	 * } else if (device.isTablet()) {
	 * audience = AUDIENCE_TABLET;
	 * } else if (device.isMobile()) {
	 * audience = AUDIENCE_MOBILE;
	 * }
	 * return audience;
	 * }
	 */

	private Boolean ignoreTokenExpiration(final String token)
	{
		final String audience = getAudienceFromToken(token);
		return (AUDIENCE_TABLET.equals(audience) || AUDIENCE_MOBILE.equals(audience));
	}

	// 按照传入的用户userDetails和Token的规则生成令牌
	public String generateToken(final UserDetails userDetails)
	{
		final Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, userDetails.getUsername());
	}

	// 生成令牌
	private String doGenerateToken(final Map<String, Object> claims, final String subject)
	{
		final Date createdDate = clock.now();
		final Date expirationDate = calculateExpirationDate(createdDate);

		System.out.println("doGenerateToken " + createdDate);

		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(createdDate).setExpiration(expirationDate).signWith(SignatureAlgorithm.HS512, secret).compact();
	}

	public Boolean canTokenBeRefreshed(final String token, final Date lastPasswordReset)
	{
		final Date created = getIssuedAtDateFromToken(token);
		return !isCreatedBeforeLastPasswordReset(created, lastPasswordReset) && (!isTokenExpired(token) || ignoreTokenExpiration(token));
	}

	public String refreshToken(final String token)
	{
		final Date createdDate = clock.now();
		final Date expirationDate = calculateExpirationDate(createdDate);

		final Claims claims = getAllClaimsFromToken(token);
		claims.setIssuedAt(createdDate);
		claims.setExpiration(expirationDate);

		return Jwts.builder().setExpiration(expirationDate).setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
	}

	public Boolean validateToken(final String token, final UserDetails userDetails)
	{
		final JwtUser user = (JwtUser) userDetails;
		final String username = getUsernameFromToken(token);
		// final Date created = getIssuedAtDateFromToken(token);
		// final Date expiration = getExpirationDateFromToken(token);
		return (username.equals(user.getUsername()) && !isTokenExpired(token));
	}

	private Date calculateExpirationDate(final Date createdDate)
	{
		return new Date(createdDate.getTime() + expiration * 1000);
	}
}