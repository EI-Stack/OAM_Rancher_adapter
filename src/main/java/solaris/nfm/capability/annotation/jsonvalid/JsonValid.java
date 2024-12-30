package solaris.nfm.capability.annotation.jsonvalid;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({METHOD})
public @interface JsonValid
{
	// 當 annotation 中只有唯一的名叫 value 的屬性時，可以只寫屬性值傳參
	// 此處 value 代表 json schema URI
	public String value() default "";
}