using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GameState : MonoBehaviour
{
    public Player player;

    public Stage stage;

    public Victory victoryHitBox;

    public SpriteRenderer victoryText;
    void Update()
    {
        if (Victory())
        {
            victoryText.enabled = true;
        }
    }

    public bool Victory()
    {
        return victoryHitBox.victory;
    }

    public void Reset()
    {
        player.Reset();
        stage.Reset();
        victoryText.enabled = false;
    }
}
