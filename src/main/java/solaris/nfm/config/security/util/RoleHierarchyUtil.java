package solaris.nfm.config.security.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

public class RoleHierarchyUtil
{
	public static RoleHierarchy getRoleHierarchyFromMap(final Map<String, List<String>> roleHierarchyMap)
	{
		StringWriter roleHierachyDescriptionBuffer = new StringWriter();
		PrintWriter roleHierarchyDescriptionWriter = new PrintWriter(roleHierachyDescriptionBuffer);

		for (Map.Entry<String, List<String>> entry : roleHierarchyMap.entrySet())
		{
			String currentRole = entry.getKey();
			List<String> impliedRoles = entry.getValue();

			for (String impliedRole : impliedRoles)
			{
				String roleMapping = currentRole + " > " + impliedRole;
				roleHierarchyDescriptionWriter.println(roleMapping);
			}
		}

		RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
		roleHierarchy.setHierarchy(roleHierachyDescriptionBuffer.toString());
		return roleHierarchy;
	}
}
