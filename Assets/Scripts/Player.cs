using UnityEngine;
using System.Collections;

public class Player : MonoBehaviour
{  
    public bool freezePlayer = false;
    public bool clamping = false;
    public Rigidbody2D rb;
    public float groundAcceleration;
    public float airAcceleration;
    public float maxSpeed;
    public int collisions;

    private float originalGravityScale;

    public Vector2 initialPosition;


    void Start()
    {
        originalGravityScale = rb.gravityScale;
    }

    public void Move(float direction)
    {
        if (OnPlatform())
        {
            rb.AddForce(new Vector2(groundAcceleration * direction, 0));
        }
        else
        {
            rb.AddForce(new Vector2(airAcceleration * direction, 0));
        }
        float clampedHorizontal = Mathf.Clamp(rb.velocity.x, -maxSpeed, maxSpeed);
        rb.velocity = new Vector2(clampedHorizontal, rb.velocity.y);
    }

    public void ClampPlayer()
    {
        rb.gravityScale = 0;
    }

    public void UnClampPlayer()
    {
        rb.gravityScale = originalGravityScale;
    }

    public void FreezePlayer()
    {
        freezePlayer = true;
        rb.velocity = new Vector2(0, 0);
    }

    public void UnFreezePlayer()
    {
        freezePlayer = false;
    }

    void OnCollisionEnter2D(Collision2D other)
    {
        if (other.collider.GetComponent<Platform>())
        {
            collisions ++;
        }
    }

    void OnCollisionExit2D(Collision2D other)
    {
        if (other.collider.GetComponent<Platform>())
        {
            collisions --;
        }
    }

    public bool OnPlatform()
    {
        return collisions > 0;
    }

    public void Reset()
    {
        this.transform.position = initialPosition;
    }
}