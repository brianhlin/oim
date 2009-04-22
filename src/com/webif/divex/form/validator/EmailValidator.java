package com.webif.divex.form.validator;

public class EmailValidator implements IFormElementValidator<String>
{
	static private EmailValidator singleton = new EmailValidator();
	static public EmailValidator getInstance() { return singleton; }
	
	public Boolean isValid(String value) {
		return org.apache.commons.validator.EmailValidator.getInstance().isValid(value);
	}
	
	public String getMessage()
	{
		return "Please specify a valid email address.";
	}
}