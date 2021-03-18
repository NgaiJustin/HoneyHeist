using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class InputController : MonoBehaviour
{
    public bool clampButton;
    public Stage stage;
    public Player player;
    public PlayerRotator playerRotator;

    public GameState gameState;

    // Update is called once per frame
    void FixedUpdate()
    {
        if (Input.GetKey(KeyCode.D))
        {
            stage.Rotate(true);
            if (player.OnPlatform())
            {
                if (Input.GetKey(KeyCode.C) && clampButton)
                {
                    playerRotator.Rotate(true);
                }
                else if (!clampButton)
                {
                    playerRotator.Rotate(true);
                }
            }
        }
        else if (Input.GetKey(KeyCode.A))
        {
            stage.Rotate(false);
            if (player.OnPlatform())
            {
                if (Input.GetKey(KeyCode.C) && clampButton)
                {
                    playerRotator.Rotate(false);
                }
                else if (!clampButton)
                {
                    playerRotator.Rotate(false);
                }
            }
        }

        if (Input.GetKey(KeyCode.C) && clampButton && player.OnPlatform())
        {
            player.FreezePlayer();
            player.ClampPlayer();
        }
        else if ((!Input.GetKey(KeyCode.C) && clampButton) || !player.OnPlatform())
        {
            player.UnClampPlayer();
            player.UnFreezePlayer();
        }
        
        
        if (Input.GetKey(KeyCode.LeftArrow) && !playerRotator.rotating && !player.freezePlayer)
        {
            player.Move(-1);
        }
        else if(Input.GetKey(KeyCode.RightArrow) && !playerRotator.rotating && !player.freezePlayer)
        {
            player.Move(1);
        }
        else
        {
            player.Move(0);
        }
        if (!player.OnPlatform())
        {
            playerRotator.StopRotation();
        }

        if (Input.GetKey(KeyCode.R))
        {
            gameState.Reset();
        }
    }
}
