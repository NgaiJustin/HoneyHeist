using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Victory : MonoBehaviour
{
    public bool victory;

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
