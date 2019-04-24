package node.clientmessaging.messages;

import static node.clientmessaging.messages.ClientMessageType.ERROR;

public class ErrorMessage extends ClientMessage {

   private String errorMessage;

   public ErrorMessage(String errorMessage) {
      super(ERROR);
      this.errorMessage = errorMessage;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   @Override
   public String toString() {
      return "ErrorMessage{" +
              "errorMessage='" + errorMessage + '\'' +
              '}';
   }
}
