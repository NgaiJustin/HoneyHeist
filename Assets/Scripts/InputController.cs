using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class InputController : MonoBehaviour
{
    public Stage stage;
    public Player player;
    public PlayerRotator playerRotator;

    // Update is called once per frame
    void Update()
    {
        if (Input.GetKey(KeyCode.A))
        {
            stage.Rotate(true);
            if (player.OnPlatform())
            {
                playerRotator.Rotate(true);
            }
        }
        else if (Input.GetKey(KeyCode.D))
        {
            stage.Rotate(false);
            if (player.OnPlatform())
            {
                playerRotator.Rotate(false);
            }
        }
        
        
        if (Input.GetKey(KeyCode.LeftArrow) && !playerRotator.rotating)
        {
            player.Move(-1);
        }
        else if(Input.GetKey(KeyCode.RightArrow) && !playerRotator.rotating)
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
    }
}
