using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Victory : MonoBehaviour
{
    public bool victory {get; private set;}

    void Start()
    {
        victory = false;
    }

    void OnTriggerEnter2D(Collider2D other)
    {
        if (other.GetComponent<Player>())
        {
            victory = true;
        }
    }
}
