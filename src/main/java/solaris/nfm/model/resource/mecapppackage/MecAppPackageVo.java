package solaris.nfm.model.resource.mecapppackage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(
{"entityVersion", "humanReadableEntityName", "humanReadableEntityBrief"})
public class MecAppPackageVo extends MecAppPackage
{
	private static final long serialVersionUID = 1L;
}