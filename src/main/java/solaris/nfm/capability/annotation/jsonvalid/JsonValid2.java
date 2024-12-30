package solaris.nfm.capability.annotation.jsonvalid;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 這是繼承 @Valid 的範例，沒有使用 AOP，此 class 沒有真正用於系統
 *
 * @author Holisun
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JsonValidationImpl.class)  // 指定自定義的驗證實做類
public @interface JsonValid2
{
	/**
	 * 驗證不通過時的報錯訊息
	 *
	 * @return 驗證不通過時的報錯訊息
	 */
	String message() default "Invalid url";

	/**
	 * 將 validator 進行分類，不同的類group中會執行不同的 validator 操作
	 *
	 * @return validator 的分類型別
	 */
	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}