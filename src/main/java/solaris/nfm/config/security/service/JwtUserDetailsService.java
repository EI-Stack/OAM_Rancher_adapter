package solaris.nfm.config.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtUserDetailsService implements UserDetailsService
{
	private static final Logger	logger	= LoggerFactory.getLogger(JwtUserDetailsService.class);
	//	@Autowired
	//	private UserDaoService		userDaoService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException
	{
		//		logger.debug("\t [JWT] username=[{}]", username);
		//		logger.debug("\t [JWT] pw=[{}]", passwordEncoder.encode("holisun"));
		// 搜尋資料庫中符合使用者登入名稱，可以藉由 DAO 使用 JDBC 來連接資料庫
		/*
		 * solaris.nfm.repository.entity.User user =
		 * this.userDaoService.findOneByName(username); if (user == null) { throw new
		 * UsernameNotFoundException("user(" + username +
		 * ") not found in the database !!"); } return JwtUserFactory.create(user,
		 * userDaoService, 0L);
		 */
		return null;
	}
}
