package solaris.nfm.config.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试授权控制器
 */
@RestController
@RequestMapping("/api")
public class MethodProtectedRestController
{
	// 只有角色是ADMIN的才能请求/protectedadmin
	@RequestMapping(value = "/protectedadmin", method = RequestMethod.GET)
	// @PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getProtectedAdmin()
	{
		return ResponseEntity.ok("Greetings from admin protected method!");
	}

	// 只有角色是USER的才能请求/protectedadmin
	@RequestMapping(value = "/protecteduser", method = RequestMethod.GET)
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> getProtectedUser()
	{
		return ResponseEntity.ok("Greetings from user protected method!");
	}
}
