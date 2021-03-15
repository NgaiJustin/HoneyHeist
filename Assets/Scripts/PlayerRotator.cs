using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class PlayerRotator : MonoBehaviour
{
    public bool rotating = false;
    const int ROTATION_DEGREES = 60;
    const float DURATION_OF_ROTATION = .5f;
    Coroutine currentCoroutine;

    public Player player;

    public float originalGravityScale;

    public void Rotate(bool left)
    {
        float rotation = ROTATION_DEGREES;
        if (left) rotation *= -1;

        if (!rotating)
        {
            if (currentCoroutine != null)
            {
                StopCoroutine(currentCoroutine);
            }
            currentCoroutine = StartCoroutine(RotationLerp(this.transform, rotation, DURATION_OF_ROTATION));
        }
    }

    public void StopRotation()
    {
        if (currentCoroutine != null)
        {
            StopCoroutine(currentCoroutine);
        }
        player.rb.gravityScale = originalGravityScale;
        rotating = false;
    }   

    private IEnumerator RotationLerp(Transform transform, float rotation, float duration)
    {
        rotating = true;
        float originalGravityScale = player.rb.gravityScale;
        player.rb.gravityScale = 0;
        Quaternion initialRotation = transform.rotation;
        Quaternion rotationTarget = Quaternion.Euler(0,0,transform.eulerAngles.z + rotation);
        float t = 0;
        while(t < duration)
        {
            t += Time.deltaTime;
            transform.rotation = Quaternion.Slerp(initialRotation, rotationTarget, t/duration);
            yield return null;
        }
        player.rb.gravityScale = originalGravityScale;
        rotating = false;
        yield return null;
    }
}
