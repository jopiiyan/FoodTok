package com.example.foodtok.services;

/** Factory singleton that provides the active {@link IInteractionService} implementation. */
public class InteractionServiceProvider {
  private static IInteractionService interactionService;
  private InteractionServiceProvider(){

  }

  public static IInteractionService getInteractionService(){
    if(interactionService == null){
      interactionService = new MockInteractionService();
    }
    return interactionService;
  }

  public static void setInteractionService(IInteractionService service){
    interactionService = service;
  }
}
